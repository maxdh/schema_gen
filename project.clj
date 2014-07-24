(defproject kixi/schema_gen "0.1.1"
  :description "Data generation from Prismatic schemas"
  :url "https://github.com/maxdh/schema_gen.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
	         [prismatic/schema "0.2.4"]
		 [org.clojure/test.check "0.5.8"]]

  :scm {:name "git"
        :url "git@github.com:maxdh/schema_gen.git"})
