(ns harmony.bookings.service
  (:require [harmony.bookings.store :as store]
            [harmony.bookings.db.bookable :as db.bookable]
            [clj-time.core :as t]
            [clj-time.periodic :as periodic]
            [clj-time.coerce :as coerce]))

(defn- bookable-defaults [m-id ref-id author-id]
  (let [plan {:marketplaceId m-id
              :seats 1
              :planMode :available}
        bookable {:marketplaceId m-id
                  :refId ref-id
                  :authorId author-id
                  :unitType :day
                  :activePlan plan}]
    {:bookable bookable
     :plan plan}))

(defn create-bookable
  [db create-cmd]
  (let [{:keys [marketplaceId refId authorId]} create-cmd]
    (when-not (db.bookable/contains-bookable?
               db
               {:marketplaceId marketplaceId :refId refId})
      (let [{:keys [bookable plan]}
            (bookable-defaults marketplaceId refId authorId)
            _ (db.bookable/create-bookable db bookable plan)
            {:keys [bookable active-plan]}
            (db.bookable/fetch-bookable-with-plan db {:marketplaceId marketplaceId :refId refId})]
        (assoc bookable :activePlan active-plan)))))

(defn fetch-bookable
  [db m-id ref-id]
  (when-let [{:keys [bookable active-plan]}
             (db.bookable/fetch-bookable-with-plan
              db
              {:marketplaceId m-id :refId ref-id})]
    (assoc bookable :activePlan active-plan)))


(defn- midnight-date-time [inst]
  (let [dt (coerce/to-date-time inst)
        [year month day] ((juxt t/year t/month t/day) dt)]
    (t/date-midnight year month day)))

(defn- free-dates [start end bookings]
  (let [booking-is (map #(t/interval (midnight-date-time (:start %))
                                     (midnight-date-time (:end %)))
                        bookings)
        booked? (fn [dt]
                  (let [day-i (t/interval dt (t/plus dt (t/days 1)))]
                    (some #(t/overlaps? day-i %) booking-is)))]
    (->> (periodic/periodic-seq
          (midnight-date-time start)
          (midnight-date-time end)
          (t/days 1))
         (remove booked?))))

(defn- time-slot [ref-id date-time]
  {:id (java.util.UUID/randomUUID)
   :refId ref-id
   :unitType :day
   :seats 1
   :start (coerce/to-date date-time)
   :end (-> date-time
            (t/plus (t/days 1))
            (coerce/to-date))
   :year (t/year date-time)
   :month (t/month date-time)
   :day (t/day date-time)})


(defn calc-free-time-slots
  [db {:keys [marketplaceId refId start end]}]
  (when-let [{:keys [bookable]} (store/fetch-bookable
                                 db
                                 {:m-id marketplaceId :ref-id refId})]
    (let [bookings (store/fetch-bookings db {:bookable-id (:id bookable)
                                             :start start
                                             :end end})]
         (->> (free-dates start end bookings)
              (map #(time-slot refId %))))))

(defn- booking-defaults
  [{:keys [m-id bookable-id customer-id initial-status start end]}]
  {:marketplaceId m-id
   :bookableId bookable-id
   :customerId customer-id
   :status initial-status
   :seats 1
   :start start
   :end end})

(defn initiate-booking [db cmd]
  (let [{:keys [marketplaceId refId customerId initialStatus start end]} cmd]
    (when-let [{:keys [bookable]} (store/fetch-bookable
                                   db
                                   {:m-id marketplaceId :ref-id refId})]
      (store/insert-booking db (booking-defaults {:m-id marketplaceId
                                                  :bookable-id (:id bookable)
                                                  :customer-id customerId
                                                  :initial-status initialStatus
                                                  :start start
                                                  :end end})))))

(comment
  (def db (store/new-mem-booking-store))
  (create-bookable db {:marketplaceId 1234 :refId 4444 :authorId 27272})

  (t/day (t/date-midnight 2016 8 31))
  (free-dates #inst "2016-09-09T10:02:01"
              #inst "2016-09-15")
  )