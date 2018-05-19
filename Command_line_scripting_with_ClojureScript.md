# Command line scripting with ClojureScript

Thanks to the recent developments from the ClojureScript community, writing command-line scripts in Clojure has been a fun experience for me. Major kudos to @anmonteiro for developing lumo and the core ClojureScript team.

I think Clojure is a great language for writing single-use scripts to process data because of the built-in manipulation functions and immutable structures so you won’t have to worry about references and deep-cloning.

## Running scripts with Lumo
The easiest way to get started is to install lumo on your system and run Clojure files with it (see NPM section below if you don’t want to install lumo globally). Create a file called hello.cljs with the following contents:

```
(println "Hello World!")
```

and run it using:

```
$ npm i -g lumo-cljs ## or other package manager of your choice
$ lumo hello.cljs
Hello World!
```

Couldn’t be easier. Now let’s look at how we can leverage Node.JS APIs to write a slightly more practical program:

```
;; The following program parses a json payload generated
;; by https://randomuser.me and extracts a subset of the data
;; $ wget 'https://randomuser.me/api/?results=10' -O randomUsers.json

;; NOTE: don't forget the single-quote in front of each require
(require
  '[clojure.string :refer [capitalize]]   ;; Require a Clojure library
  '[fs :refer [writeFileSync]]            ;; Require a Node.JS module (filesystem)
  '["./randomUsers.json" :as input-json]  ;; Require a JSON file as JS data  
  )

(defn parse-user
  [{:keys [email name]
    {:keys [username password]} :login}]
  {:id         (str (random-uuid)) ;; built-in
   :username   username
   :password   password
   :email      email
   :full-name  (str (capitalize (:first name))
                    " "
                    (capitalize (:last name)))
   })

(let [results (:results (js->clj input-json :keywordize-keys true))]
  (writeFileSync
    "randomUsers.edn"
    (pr-str (mapv parse-user results))))
```



As you can see, the require function can now act like its JS counterpart and pull in JS modules as you would with the following code:

```
const { writeFileSync } = require('fs');
const inputJson = require('./randomUsers.json');
```

It also works with modules inside the node_modules folder installed using npm in the same directory (as of lumo 1.8.0). Unfortunately, there isn’t an easy way to manage dependencies on the Clojure side just yet so for now we are stuck with manually handling JAR files where Clojure libraries are usually packaged. (Take a look at the lumo wiki for more details.)

## Integrating with NPM
For slightly larger projects you’d probably want to have a proper package.json with some dependencies on NPM. You could also install lumo on a per-project basis if you don’t want to install it globally or wish to publish the package elsewhere. Here’s a sample project that works like the above example but fetches JSON from https://randomuser.me using the request library from NPM and splits the code into 2 Clojure files with proper name-spacing:

```
my-tool
|\_ package.json
 \_ src
     \_ my_tool
        |\_ core.cljs
         \_ user.cljs
```

Clojure’s namespace system mirrors the directory structure so the file with the ns `my-tool.core` must be `my_tool/core.cljs`.

Gotcha: Hyphen delimited names in the namespace MUST be converted into snake_case in the filesystem.

core.cljs:

```
(ns my-tool.core
  (:require [my-tool.user :as user]
            [fs :refer [writeFileSync]]
            ;; request library from npm, since the imported name
            ;; is the same as the package name, :as isn't needed.
            ;; const request = require('request');
            [request]))

;; The -main function is called by lumo with CLI args
(defn -main [n]
  (request
    #js{:url (str "https://randomuser.me/api/?results=" n)
        :json true}
    (fn [_ _ body]
      (let [results (:results (js->clj body :keywordize-keys true))]
        (writeFileSync
          "randomUsers.edn"
          (pr-str (mapv user/parse results)))))))
```

user.cljs:
```
(ns my-tool.user
  (:require [clojure.string :refer [capitalize]]))

(defn parse
  [{:keys [email name]
    {:keys [username password]} :login
    }]
  {:id         (str (random-uuid))
   :username   username
   :password   password
   :email      email
   :full-name  (str (capitalize (:first name))
                    " "
                    (capitalize (:last name)))
   })
```


package.json:
```
{
  "name": "my-tool",
  "version": "1.0.0",
  "scripts": {
    "start": "lumo -c src -m my-tool.core"
  },
  "dependencies": {
    "lumo-cljs": "^1.8.0",
    "request": "^2.85.0"
  }
}
```

The -c flag tells lumo where your source files are and the -m flag specifies which namespace your -main function is in. You can run this tool using the usual npm procedures:

```
$ npm install
$ npm start 12 ## fetches 12 users and outputs randomUsers.edn
```

## REPL Development
Of course, no Clojure experience would be complete without interactive REPL-based development. Add the following line into the scripts section of your package.json file:

```
"repl": "lumo -c src -i src/my_tool/core.cljs -n 5777 -r"
```

The -i flag initializes the REPL with our entry point core.cljs, the -n flag starts a socket REPL on port 5777 for editor integration and finally the -r flag start a REPL in the terminal. With this you could execute arbitrary code in your runtime without loosing state:

```
$ npm run repl
...
cljs.user=> (in-ns 'my-tool.core) ;; switch to our core namespace
my-tool.core=> (user/parse {:name {:first "john" :last "smith"}})
{:id "c1b61773-133e-434c-afbd-d82b95b814d3",
 :username nil,
 :password nil,
 :email nil,
:full-name "John Smith"}
```

## Final Thoughts
While there are still a few rough edges with the current tooling, I think this is the time where writing CLI scripts in Clojure starts to become a viable option and a nice alternative to JS.

Lumo’s startup time is blazing fast compared to any clojur-y things running on the JVM so it’s a breath of fresh air.

I would definitely recommend trying this out if you enjoy Clojure.

