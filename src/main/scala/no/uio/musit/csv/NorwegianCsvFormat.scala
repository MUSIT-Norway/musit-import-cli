package no.uio.musit.csv

import com.github.tototoshi.csv.DefaultCSVFormat

trait NorwegianCsvFormat extends DefaultCSVFormat {

  override val delimiter = ';'

}

object NorwegianCsvFormat extends NorwegianCsvFormat
