(ns schema-gen.core
  "Methods to generate examples that follow a supplied Prismatic Schema.
   This is mostly code from a Gist by Dave Golland with a few additions by me to support additional types and schema elements."
  (:require
   [schema.core :as s]
   [clojure.pprint :as pprint]
   [clojure.test.check.generators :as gen]))

(defn g-by [f & args]
  (gen/fmap
   (partial apply f)
   (apply gen/tuple args)))

(defn g-apply-by [f args]
  (gen/fmap f (apply gen/tuple args)))

(defn g-or [& args]
  (gen/one-of args))

(defprotocol Generatable
  (generate [x] "return a generator for x"))

(extend-type Object
  Generatable
  (generate [x]
    (cond
     (= x s/Int) gen/int
     (= x s/Bool) gen/boolean
     (= x Boolean) gen/boolean
     (= x String) gen/string-ascii
     (= x s/Keyword) gen/keyword
     (= x Number) gen/s-neg-int ;no generator for doubles, so just generate a negative int, is this good enough? Otherwise could import clojure.data.generators and use that for more options.
     :else (gen/return x))))

(extend-type schema.core.Either
  Generatable
  (generate [x]
    (gen/one-of (map generate (:schemas x)))))

(extend-type schema.core.One
  Generatable
  (generate [x]
    (generate (:schema x))))

(extend-type schema.core.EqSchema
  Generatable
  (generate [x]
    (gen/return (:v x))))

(extend-type schema.core.EnumSchema
  Generatable
  (generate [x]
    (gen/one-of (map generate (:vs x)))))

(comment
  "not working - seems to read other elemnts as predicates too ..."
  (extend-type schema.core.Predicate
    Generatable
    (generate [x]
      (partial (gen/such-that (generate (:p? x)))))))


(extend-type clojure.lang.APersistentVector
  Generatable
  (generate [x]
    (let [[ones [repeated]] (split-with #(instance? schema.core.One %) x)
          [required optional] (split-with (comp not :optional?) ones)]
      
      (g-by
       concat
       (g-or
        (apply gen/tuple (map generate required))
        (apply gen/tuple (map generate (concat required optional))))
       (if repeated
         (gen/vector (generate repeated))
         (gen/return []))))))

(extend-type schema.core.RequiredKey
  Generatable
  (generate [x]
    (gen/return (:k x))))

(extend-type schema.core.OptionalKey
  Generatable
  (generate [x]
    (gen/return (:k x))))

(defn optional-entry [[k v]]
  (g-or
   (gen/return {})
   (g-by hash-map (generate k) (generate v))))

(extend-type clojure.lang.APersistentMap
  Generatable
  (generate [x]
    
    (let [[required other] (split-with
                            (fn [[k v]]
                              (or (keyword? k)
                                  (instance? schema.core.RequiredKey k))) x)
          
          [optional [repeated]] (split-with
                                 (fn [[k v]] (instance? schema.core.OptionalKey k)) other)]
      
      (g-by
       merge
       (g-apply-by (partial into {}) (map optional-entry optional))
       (if repeated
         (->> repeated (map generate) (apply gen/map))
         (gen/return {}))
       (g-apply-by
        (partial into {})
        (for [entry required]
          (apply gen/tuple (map generate entry))))))))

(defn generate-examples [schema]
  "Given a single schema, produce examples which fulfil it."
  (-> schema generate gen/sample))

(defn generate-examples-with-details [& args]
  "- Given a schema, output its details as read by s/explain and then list a few examples which fulfil the schema.
   - Can take multiple schema as input.
   - NOTE: generate-examples may work even when generate-examples-with-details does not, as not all schema methods implement 'explain'. For example, at the moment this will work:
   (generate-examples (s/one s/one \"int\"))
   whereas this will not:
   (generate-examples-with-details (s/one s/one \"int\"))"
  (map (fn [s] (doseq [schm [s]]
                 
                 (println "\n\n=== SCHEMA ===")
                 (pprint/pprint (s/explain schm))
                 (println "== Samples ==")
                 
                 (-> schm generate gen/sample pprint/pprint))) args ))


(defn validator
  "- Returns true when all generated examples can be validated by the original schema.
  - Optional count parameter for the number of examples to generate and then test.
  - NOTE: When a low number is supplied, the test.check generators only produce small examples. So in the case of strings, a small count parameter may lead to only empty strings being tested."
  ([schema]
     (validator schema 10))
  ([schema count]
     (let [samples (gen/sample (generate schema) count)]
       (every? #(s/validate schema %) samples))))
