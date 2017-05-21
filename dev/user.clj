(ns user
  (:require
   [tubular.core]))

(defn connect []
  (tubular.core/connect 49723))
