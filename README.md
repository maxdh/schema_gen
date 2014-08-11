# schema_gen

A Clojure library to generate examples which fulfil a supplied Prismatic Schema.

Leiningen dependency (Clojars): ``[kixi/schema_gen "0.1.3"]``.

This code is mostly based off of a Gist by Dave Golland (which can be found [here](https://gist.github.com/davegolland/3bc4277fe109e7b11770)) with a few additions for other schema elements and types, and a couple of methods to return a list of examples that match a schema.

Currently I would like to add functionality for ``s/pred``.

Please also note that ``generate-examples-with-details`` may fail for a given input, but ``generate-examples`` could work for the same input in some cases. For example ``s/both`` will not work with ``generate-examples-with-details`` depending on the arguments it is passed.

## Examples

```clojure
(ns gen-examples
  (:require [schema.core :as s]
	    [schema_gen.core :as sg]))

(def simpleSchema
  "A simple schema to generate from"
  {:name s/Str
   :number s/Int})

(sg/generate-examples simpleSchema)
;; ({:num 0, :name ""} {:num 0, :name "O"} {:num -2, :name ""} {:num 0, :name "x"} {:num 4, :name "ci"} {:num 0, :name "h"} {:num 3, :name "-1g^Js"} {:num -5, :name "6i$v!"} {:num -5, :name "}C"} {:num 7, :name ">XX(#|V"})

(sg/generate-examples-with-details (s/enum "Hello" "World" s/Bool))
;; === SCHEMA ===
;; (enum "World" java.lang.Boolean "Hello")
;; == Samples ==
;; ("Hello" "Hello" true false "Hello" "World" false true true false)
```

#### Dates
There is also support for ``ISO-Date-Time`` generation from the [schema-contrib](https://github.com/sfx/schema-contrib) library.

```clojure
(ns gen-examples
  (:require [schema.core :as s]
	    [schema_gen.core :as sg]
	    [schema-contrib.core :as sc])

(def dateSchema
  {:name s/Str
   :date sc/ISO-Date-Time})

(sg/generate-examples-with-details dateSchema)
;; === SCHEMA ===
;; {:name java.lang.String, :age Int, :date (pred ISO-Date-Time)}
;; == Samples ==
;; ({:date "1985-03-12T21:11:27.807Z", :age 0, :name ""}
;; {:date "1928-12-22T10:16:24.168Z", :age 0, :name "7"}
;; {:date "1945-10-23T05:40:27.775Z", :age 2, :name "W"}
;; {:date "2046-06-25T07:33:01.036Z", :age -3, :name ""}
;; {:date "1928-11-30T19:19:15.086Z", :age -4, :name "4&P"}
;; {:date "1968-05-14T02:47:39.049Z", :age -2, :name "r?i"}
;; {:date "1935-05-02T08:05:22.810Z", :age 1, :name "("}
;; {:date "2034-08-30T02:53:24.470Z", :age -2, :name ""}
;; {:date "2012-01-08T12:32:19.297Z", :age -2, :name "_g/"}
;; {:date "1960-03-13T05:10:00.723Z", :age 7, :name "\"23v"})
```

#### Both
There is some limited support for ``s/both``. The schema has to follow a certain format to use it, but can be very useful. For example, it can be used to generate non-empty strings while still being a Prismatic Schema.

The format must match:
``(s/both pred object)``
Where pred is a simple function such as ``odd?``, ``not-empty`` or something you define yourself. Object must be a schema object to generate such as ``s/Int``, ``s/Str``, ``s/Bool`` etc.

The output of ``(sg/generate-examples s/Str)`` could be:
``("" "" "" "" "1gd3" "QAM" "Rt " "^$" "W&GJUF0" "D*GiET")``

In some cases such as feeding the generated data into a database you may not allow empty strings, which is why ``s/both`` can be useful. It can also be used to generated only odd numbers, numbers greater than 5 etc.

```clojure
(ns gen-examples
  (:require [schema.core :as s]
	    [schema_gen.core :as sg]
	    [schema-contrib.core :as sc])

(def bothSchema
  {:name (s/both not-empty s/Str)
   :address s/Str})

;;generate-examples-with details cannot be used on 'both'
(sg/generate-examples bothSchema)
;; ({:name "*?", :address ""} {:name "FR", :address "a"} {:name "C", :address "RM"} {:name "tP)", :address "]qI"} {:name "z", :address ""} {:name "MQ\\", :address "iVbnF"} {:name ";bx}R.", :address "7.6-{"} {:name "ibcue|d", :address "[_91"} {:name "pn", :address "L"} {:name "*d$!5O}-", :address "o,%iA)"})

(sg/generate-examples (s/both #(> % 5) s/Int))
;; (7 8 8 6 7 8 7 8 6 8)

(sg/generate-examples (s/both odd? s/Int))
;; (-3 -1 -1 9 1 -1 -5 7 -1 5)
```

Currently ``generate-examples-with-details`` and ``validator`` cannot be used with ``both``. Also ``both`` uses test.check's ``gen/such-that`` which has a maximum number of retries, so it can fail sometimes. Especially if the first argument is hard to fulfil.

## License

Copyright © 2014 Mastodon C Ltd

Distributed under the Eclipse Public License version 1.0.

