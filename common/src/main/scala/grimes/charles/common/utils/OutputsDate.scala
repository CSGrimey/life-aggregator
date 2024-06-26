package grimes.charles.common.utils

import java.time.format.DateTimeFormatter

trait OutputsDate {
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
}