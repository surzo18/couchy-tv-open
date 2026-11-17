package com.conreo.couchytv

object WeatherUnits {
    fun temperature(celsius:Double,imperial:Boolean)=if(imperial)celsius*9.0/5.0+32.0 else celsius
    fun wind(kmh:Double,imperial:Boolean)=if(imperial)kmh/1.609344 else kmh
}
