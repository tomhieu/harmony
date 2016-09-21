(ns harmony.service.conn-pool
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :as hikari]

            [harmony.util.log :as log]
            [harmony.config :as config]))

(defrecord ConnPool [datasource pool-config]
  component/Lifecycle
  (start [component]
    (if-not datasource
      (let [cp (hikari/make-datasource
                (merge pool-config {:adapter "mysql"}))]
        (log/info :connection-pool :started)
        (assoc component :datasource cp))
      component))
  (stop [component]
    (if datasource
      (do
        (log/info :connection-pool :stopping)
        (hikari/close-datasource datasource)
        (log/info :connection-pool :stopped)
        (assoc component :datasource nil))
      component)))

(defn new-connection-pool [pool-config]
  (map->ConnPool {:pool-config pool-config}))

