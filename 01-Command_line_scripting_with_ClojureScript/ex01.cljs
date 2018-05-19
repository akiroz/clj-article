;; 次のプログラムでは、https://randomuser.meで生成されたjsonのペイロードをパーズして、
;; データの一部を抽出します。予め、次のコマンドでデータをダウンロードしてください。
;; $ wget 'https://randomuser.me/api/?results=10' -O randomUsers.json

;; NOTE: don't forget the single-quote in front of each require
(require
  '[clojure.string :refer [capitalize]]   ;; Clojureのライブラリをrequireします
  '[fs :refer [writeFileSync]]            ;; Node.JSのfilesystem関連のモジュールをrequireします
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