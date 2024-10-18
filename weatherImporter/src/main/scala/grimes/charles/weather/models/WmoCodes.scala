package grimes.charles.weather.models

object WmoCodes {
  val mapping = Map[Int, String](
    0 -> "Clear",
    1 -> "Mostly clear",
    2 -> "Partly cloudy",
    3 -> "Overcast",
    45 -> "Fog",
    48 -> "Icy fog",
    51 -> "Light drizzle",
    53 -> "Drizzle",
    55 -> "Heavy drizzle",
    56 -> "Light freezing drizzle",
    57 -> "Freezing drizzle",
    61 -> "Light rain",
    63 -> "Rain",
    65 -> "Heavy rain",
    66 -> "Light freezing rain",
    67 -> "Freezing rain",
    71 -> "Light snow",
    73 -> "Snow",
    75 -> "Heavy snow",
    77 -> "Snow flakes",
    80 -> "Light showers",
    81 -> "Showers",
    82 -> "Heavy showers",
    85 -> "Light snow showers",
    86 -> "Snow showers",
    95 -> "Thunderstorm",
    96 -> "Light thunderstorm with hail",
    99 -> "Thunderstorm with hail"
  )
}