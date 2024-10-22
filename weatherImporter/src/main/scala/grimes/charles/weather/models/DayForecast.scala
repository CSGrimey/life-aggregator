package grimes.charles.weather.models

import cats.data.NonEmptyList

import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date

case class DayForecast(date: Date, sunrise: LocalTime, sunset: LocalTime, hourlyForecast: NonEmptyList[HourForecast]) {
  override def toString: String =
    s"""
      ${new SimpleDateFormat("dd-MM-yyyy").format(date)}<br>
      Sunrise = $sunrise | Sunset = $sunset<br>
      ${hourlyForecast.toList.map(hf => { import hf._; s"$time $temperature $weather<br>" }).mkString}
    """
}

case class HourForecast(temperature: String, time: LocalTime, weather: String)