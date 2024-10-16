package grimes.charles.weather

import cats.effect.Async
import org.http4s.implicits.uri

class WeatherService[F[_] : Async] {
  // https://open-meteo.com/en/docs
  private val weatherUrl = uri"https://api.open-meteo.com/v1/forecast?latitude=51.45625&longitude=-0.97113&hourly=temperature_2m,weather_code&daily=sunrise,sunset&timezone=Europe%2FLondon&forecast_days=1&models=ukmo_seamless"

}