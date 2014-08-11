(defproject kixi/schema_gen "0.1.4"
  :description "Data generation from Prismatic schemas"
  :url "https://github.com/maxdh/schema_gen.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
	         [prismatic/schema "0.2.6"]
		 [org.clojure/test.check "0.5.9"]
                 [schema-contrib "0.1.3"]
                 [clj-time "0.7.0"]]

  :scm {:name "git"
        :url "git@github.com:maxdh/schema_gen.git"})
