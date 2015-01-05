(defproject markov-cloj "0.1.0-SNAPSHOT"
  :description "Markov Chain Generator for Text"
  :url "https://github.com/davidhampgonsalves/markov-cloj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.tumblr/jumblr "0.0.6"]]
  :main ^:skip-aot markov-cloj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
						 :markov {:main markov-cloj.core}
           	 :tumblr {:main markov-cloj.postr}}
 	:aliases {"markov" ["with-profile" "markov" "run"]
           "tumblr" ["with-profile" "tumblr" "run"]})
