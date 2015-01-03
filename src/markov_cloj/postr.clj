(ns markov-cloj.postr
  (gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
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
                       {:xauth {:email "" :password ""} 
                        :consumer {:key "" :secret ""}
                        :blog {:name ""}}))))))

(defn format-post [msg]
  (-> msg
      (str/replace #"(:[^\s]+)" "<code>$1</code>")
      (str/replace #"`([^`]+)`" "<code>$1</code>")
      (str/replace #" ([^ ]+-[^ ]+) " "<code>$1</code>")))

(defn post [msg]
  "Posts a quote to the set tumblr blog"
  (let [config (read-config)
        client (new JumblrClient (get-in config [:consumer :key]) 
                    (get-in config [:consumer :secret]))
        details {"" msg}]
    (.xauth (get-in config [:xauth :email]) (get-in config [:xauth :password]))
    (.postCreate client (get-in config [:blog :name]) details)))
