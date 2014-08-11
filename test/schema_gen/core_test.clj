(ns schema-gen.core-test
  (:require [clojure.test :refer :all]
            [schema_gen.core :refer :all]
            [schema.core :as s]
            [schema-contrib.core :as sc]
            ))

(def ^:private test-map-schema
  {:string s/Str
   :int s/Int
   :nestedmap {:num s/Num
               :key s/Keyword}
   :date sc/ISO-Date-Time})

(deftest map-test
  (is (true? (validator test-map-schema))))

(def ^:private test-vector-schema
  [(s/one s/Str "Str")
   (s/one s/Int "Int")
   (s/one s/Num "Num")
   sc/ISO-Date-Time])

(deftest vector-test
  (is (true? (validator test-vector-schema))))
