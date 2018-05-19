# Command line scripting with ClojureScript

ClojureScriptコミュニティによる開発のおかげで、コマンドライン・スクリプトをClojureで書くのが楽しくなってきました。ClojureScriptの中心チームでlumoを開発している[anmonteiro](https://github.com/anmonteiro)さんには、心から敬意を表します。

Clojureは、データを処理するために使い捨てのスクリプトを書くのに良い言語だと思います。というのも、組み込みで操作用の関数とイミュータブルな構造をもっていて、参照性やdeep-cloningについて心配する必要がないからです。

## Lumoでスクリプトを起動する
始めるために簡単な方法を紹介します。lumoをシステムにインストールして、Clojureのファイルを起動してみましょう。（まだ、lumoをインストールしていないようであれば、下のNPMのセクションをご参照ください。）`hello.cljs`という名前でファイルを作り、次のように編集してください。

```
(println "Hello World!")
```

では、次のように実行してみましょう。

```
$ npm i -g lumo-cljs ## or other package manager of your choice
$ lumo hello.cljs
Hello World!
```

簡単でしょう？ 
では次は、より実用的なプログラムを書くために、Node.JSのAPIを利用する方法をみてみましょう。

```
;; 次のプログラムでは、https://randomuser.meで生成されたjsonのペイロードをパーズして、
;; データの一部を抽出します。予め、次のコマンドでデータをダウンロードしてください。
;; $ wget 'https://randomuser.me/api/?results=10' -O randomUsers.json

;; NOTE: don't forget the single-quote in front of each require
(require
  '[clojure.string :refer [capitalize]]   ;; Clojureのライブラリをrequireします
  '[fs :refer [writeFileSync]]            ;; Node.JSのfilesystem関連のモジュールをrequireします
  '["./randomUsers.json" :as input-json]  ;; JSONファイルをJSのデータとしてrequireします
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

ご覧の通り、このrequire関数は、JSのrequire関数と同じように動作します。次のようにすることと同じように動きます。

```
const { writeFileSync } = require('fs');
const inputJson = require('./randomUsers.json');
```

npmを用いてインストールされるnode_modulesにあるモジュールとも上手く動作します(現状lumo 1.8.0において)。
残念ながら、Clojure側での依存関係を管理する簡単な方法はありません。現状では、Clojureのライブラリが収められるjarファイルを手動で管理する必要があります。詳しくは、lumoのWikiも参考にしてください。

## NPMとの連携
For slightly larger projects you’d probably want to have a proper package.json with some dependencies on NPM. 
You could also install lumo on a per-project basis if you don’t want to install it globally or wish to publish the package elsewhere. 
Here’s a sample project that works like the above example but fetches JSON from https://randomuser.me using the request library from NPM and splits the code into 2 Clojure files with proper name-spacing:

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

The -c flag tells lumo where your source files are and the -m flag specifies which namespace your -main function is in. 
You can run this tool using the usual npm procedures:

```
$ npm install
$ npm start 12 ## fetches 12 users and outputs randomUsers.edn
```

## REPLでの開発
Of course, no Clojure experience would be complete without interactive REPL-based development. 
Add the following line into the scripts section of your package.json file:

```
"repl": "lumo -c src -i src/my_tool/core.cljs -n 5777 -r"
```

The -i flag initializes the REPL with our entry point core.cljs, the -n flag starts a socket REPL on port 5777 for editor integration and finally the -r flag start a REPL in the terminal. 
With this you could execute arbitrary code in your runtime without loosing state:

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

## さいごに
While there are still a few rough edges with the current tooling, 
I think this is the time where writing CLI scripts in Clojure starts to become a viable option and a nice alternative to JS.

Lumo’s startup time is blazing fast compared to any clojur-y things running on the JVM so it’s a breath of fresh air.

I would definitely recommend trying this out if you enjoy Clojure.

