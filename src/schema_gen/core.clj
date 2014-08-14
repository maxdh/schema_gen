(ns schema_gen.core
  "Methods to generate examples that follow a supplied Prismatic Schema.
   This is mostly code from a Gist by Dave Golland with a few additions by me to support additional types and schema elements."
  (:require
   [schema.core :as s]
   [clojure.pprint :as pprint]
   [clojure.test.check.generators :as gen]
   [schema-contrib.core :as sc]
   [clj-time.coerce :as c]))

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
     (= x Number) (gen/fmap #(* % (rand)) (gen/elements [-1 1]))
     (= x sc/ISO-Date-Time) (gen/fmap #(str (c/from-long %)) (gen/choose (- (System/currentTimeMillis)) (* 2(System/currentTimeMillis))))
     :else (gen/return x))))

(extend-type schema.core.Either
  Generatable
  (generate [x]
    (gen/one-of (map generate (:schemas x)))))

(extend-type schema.core.Maybe
  Generatable
  (generate [x]
    (g-or (gen/return nil) (generate (:schema x)))))

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

;; Both works when s/both is used with exactly two arguments,
;; where the first is a predicate such as "odd?" and the second is a
;; generatable schema type.
;; e.g. (generate-examples (s/both odd? s/Int))
(extend-type schema.core.Both
  Generatable
  (generate [x]
    (if (= (count (:schemas x)) 2) 
      (let [[first second] (:schemas x)]
        (gen/such-that first (generate second)))
      (throw (Exception. "Incorrect number of args for generation of 'both'\nMust be: (both pred object)\nSee docs for examples.") ))))

(comment
  "not working - seems to read other schema elements as predicates too ...
   For example both of the following return true:
   (instance? schema.core.Predicate s/Int)
   (instance? schema.core.Predicate (s/pred odd?))"
  (extend-type schema.core.Predicate
    Generatable
    (generate [x]
      (partial (gen/such-that (generate (:p? x)) 20)))))

(extend-type clojure.lang.APersistentVector
  Generatable
  (generate [x]
    (let [[ones [repeated]] (split-with #(instance? schema.core.One %) x)
          [required optional] (split-with (comp not :optional?) ones)]

      (g-by
       #(vec (flatten (concat %&)))
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
    
    (let [split-data  (group-by
                            (fn [[k v]]
                              (or (keyword? k)
                                  (s/required-key? k))) x)
          required    (into () (get-in split-data ['true]))
          other       (into () (get-in split-data ['false]))
          
          [optional [repeated]] (split-with
                                 (fn [[k v]] (s/optional-key? k)) other)]
      
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

(defn generate-examples
  "- Given a single schema, produce examples which fulfil it.
   - Optional count parameter for number of samples to produce"
  ([schema]
     (generate-examples schema 10))
  ([schema count]
     (gen/sample (generate schema) count)))

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
