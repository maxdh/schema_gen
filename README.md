# schema_gen

A Clojure library to generate examples which fulfil a supplied Prismatic Schema.

This code is mostly based off of a Gist by Dave Golland (which can be found [here](https://gist.github.com/davegolland/3bc4277fe109e7b11770)) with a few additions for other schema elements and types, and a couple of methods to return a list of examples that match a schema.

Currently I am trying to add functionality for `s/pred`.

Please also note that `generate-examples-with-details` may fail for a given input, but `generate-examples` could work for the same input in some cases.

## Examples

```clojure
(ns gen-examples
  (:require [schema.core :as s]
	    [schema-gen.core :as sg]))

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

## License

Copyright © 2014 Mastodon C Ltd

Distributed under the Eclipse Public License version 1.0.

