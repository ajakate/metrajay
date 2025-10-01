(ns metrajay.util
  (:require [goog.string :as gstring]
            [clojure.string :as str]
            [goog.string.format]))

(defn today-ymd []
  (let [d (js/Date.)
        yyyy (.getFullYear d)
        mm   (inc (.getMonth d))   ;; months are 0-based
        dd   (.getDate d)]
    (gstring/format "%04d-%02d-%02d" yyyy mm dd)))

(defn remap-keys-and-vals [m f]
  (into {}
        (map (fn [[k v]]
               (f k v)))
        m))

(def day-order
  {"Mon" 1
   "Tue" 2
   "Wed" 3
   "Thu" 4
   "Fri" 5
   "Sat" 6
   "Sun" 7})

(def reverse-day-order
  {1 "Mon" 2 "Tue" 3 "Wed" 4 "Thu" 5 "Fri" 6 "Sat" 7 "Sun"})

(defn compress-days [days]
  (let [nums (sort (map day-order days))]
    (loop [xs nums
           ranges []]
      (if (empty? xs)
        ;; format ranges back into strings
        (->> ranges
             (map (fn [[start end]]
                    (if (= start end)
                      (reverse-day-order start)
                      (str (reverse-day-order start) "-" (reverse-day-order end)))))
             (clojure.string/join ","))
        ;; step through consecutive numbers
        (let [[head & tail] xs
              [rng-start rng-end rest]
              (loop [cur head ys tail]
                (if (= (inc cur) (first ys))
                  (recur (first ys) (rest ys))
                  [head cur ys]))]
          (recur rest (conj ranges [rng-start rng-end])))))))

(defn zero-pad [n]
  (if (< n 10) (str "0" n) (str n)))

(defn format-time [time-str]
  (let [[h m s] (map #(js/parseInt %) (str/split time-str #":"))
        h24 (mod h 24)                       ;; wrap 25 → 1
        period (if (< h24 12) "am" "pm")
        h12 (let [hh (mod h24 12)]
              (if (zero? hh) 12 hh))]
    (str (zero-pad h12) ":" (zero-pad m) " " period)))
