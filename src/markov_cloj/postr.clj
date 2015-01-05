(ns markov-cloj.postr
  (gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [markov-cloj.core :as markov :only [get-chain]])
  (:import (com.tumblr.jumblr JumblrClient)))

(defn read-config []
  "read the tumblr.config.edn settings or creates a empty version if dosen't exist"
  (let [config (io/as-file "tumblr.config.edn")]
    (if (.exists config)
      (read-string (slurp config))
      (do
        (println "To post to tumblr you need to fill in your creds in tumblr.config.edn."
                 "A template file has been created for you.")
        (spit config (.toString 
                       {:oauth {:token "" :secret ""} 
                        :consumer {:key "" :secret ""}
                        :blog {:name ""}}))))))

(defn format-post [msg]
  (-> msg
      (str/replace #"(:[^\s]+)" "<code>$1</code>")
      (str/replace #"`([^`]+)`" "<code>$1</code>")
      (str/replace #"[’‘]" "")
      (str/replace #" ([^ ]+-[^ ]+) " " <code>$1</code> ")))

(defn post [msg]
  "Posts a message to  tumblr"
  (let [config (read-config)
        client (JumblrClient. (get-in config [:consumer :key]) 
                    (get-in config [:consumer :secret]))
        details {"body" msg}]
    (.setToken client (get-in config [:oauth :token]) 
               (get-in config [:oauth :secret]))
    (.postCreate client (get-in config [:blog :name]) details)))

(defn -main[& args] 
  "generate some text with the passed in markov chain, format it and post it to tumblr"
  (let [chain (markov/get-chain args)
    text (-> (markov/get-text chain 30)
             format-post)]
    (post text)
    (println text "\n-- was posted to Tumblr")))
