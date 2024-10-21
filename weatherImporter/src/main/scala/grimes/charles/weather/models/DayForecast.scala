package grimes.charles.weather.models

import cats.data.NonEmptyList

import java.time.LocalTime
import java.util.Date

case class DayForecast(date: Date, sunrise: LocalTime, sunset: LocalTime, hourlyForecast: NonEmptyList[HourForecast])
case class HourForecast(temperature: String, time: LocalTime, weather: String)