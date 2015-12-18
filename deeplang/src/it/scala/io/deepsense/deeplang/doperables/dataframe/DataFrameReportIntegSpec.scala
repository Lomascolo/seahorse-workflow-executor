/**
 * Copyright 2015, deepsense.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepsense.deeplang.doperables.dataframe

import java.sql.Timestamp

import scala.collection.immutable

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.joda.time.DateTime

import io.deepsense.commons.datetime.DateTimeConverter
import io.deepsense.commons.types.ColumnType
import io.deepsense.deeplang.DeeplangIntegTestSupport
import io.deepsense.deeplang.doperables.dataframe.report.DataFrameReportGenerator
import io.deepsense.reportlib.model._

class DataFrameReportIntegSpec extends DeeplangIntegTestSupport with DataFrameTestFactory {

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  "DataFrame" should {
    "generate report with data sample table" when {
      val exampleString = "DeepSense.io"
      val columnNameBase = "stringColumn"
      "number of columns and rows is minimal" in {
        val columnsNumber = 1
        val rowsNumber = 1
        testReportTables(Some(exampleString), columnNameBase, columnsNumber, rowsNumber)
      }
      "DataFrame is empty" in {
        val columnsNumber = 0
        val rowsNumber = 0
        testReportTables(Some(exampleString), columnNameBase, columnsNumber, rowsNumber)
      }
      "DataFrame has missing values" in {
        val now = DateTimeConverter.now
        val nameColumnName = "name"
        val birthDateColumnName = "birthdate"
        val rdd: RDD[Row] = sparkContext.parallelize(
          List(Row(null, new Timestamp(now.getMillis)), Row(exampleString, null)))
        val schema: StructType = StructType(List(
          StructField(nameColumnName, StringType),
          StructField(birthDateColumnName, TimestampType)))
        val dataFrame =
          executionContext.dataFrameBuilder.buildDataFrame(schema, rdd)

        val report = dataFrame.report(executionContext)

        val tables: Map[String, Table] = report.content.tables
        val dataSampleTable = tables.get(DataFrameReportGenerator.DataSampleTableName).get
        dataSampleTable.columnNames shouldBe Some(List(nameColumnName, birthDateColumnName))
        dataSampleTable.rowNames shouldBe None
        dataSampleTable.values shouldBe
          List(List(None, Some(DateTimeConverter.toString(now))), List(Some(exampleString), None))
      }
      "there is timestamp column" in {
        val now: DateTime = DateTimeConverter.now
        val timestampColumnName: String = "timestampColumn"
        val dataFrame = executionContext.dataFrameBuilder.buildDataFrame(
          StructType(List(StructField(timestampColumnName, TimestampType))),
          sparkContext.parallelize(List(Row(new Timestamp(now.getMillis)))))

        val report = dataFrame.report(executionContext)

        val tables: Map[String, Table] = report.content.tables
        val dataSampleTable = tables.get(DataFrameReportGenerator.DataSampleTableName).get
        dataSampleTable.columnNames shouldBe Some(List(timestampColumnName))
        dataSampleTable.rowNames shouldBe None
        dataSampleTable.values shouldBe List(List(Some(DateTimeConverter.toString(now))))
      }
    }
    "generate report with correct column types" in {
      val dataFrame = testDataFrame(executionContext.dataFrameBuilder, sparkContext)

      val report = dataFrame.report(executionContext)
      val tables: Map[String, Table] = report.content.tables
      val dataSampleTable = tables.get(DataFrameReportGenerator.DataSampleTableName).get

      dataSampleTable.columnTypes shouldBe List(
        ColumnType.string,
        ColumnType.boolean,
        ColumnType.numeric,
        ColumnType.timestamp,
        ColumnType.numeric)
    }
    "generate simplified report with only schema table" when {
      "number of column in schema exceeds threshold" in {
        val dataFrame = dataWithColumnsCountOverThreshold()

        val report = dataFrame.report(executionContext)

        val expectedValues = for (field <- dataFrame.schema.get.fields) yield {
          List(Some(field.name), Some(field.dataType.simpleString))
        }

        val dataTable = report.content.tables(DataFrameReportGenerator.DataSchemaTableName)
        dataTable.values shouldEqual expectedValues
        report.content.tables.get(DataFrameReportGenerator.DataSampleTableName) shouldBe None
        for(field <- dataFrame.schema.get.fields) {
          report.content.distributions(field.name) shouldBe a [NoDistribution]
        }
      }
    }
    "generate correct report" when {
      "DataFrame is empty" in {
        val schema = StructType(Seq(
          StructField("string", StringType),
          StructField("numeric", DoubleType),
          StructField("categorical", IntegerType),
          StructField("timestamp", TimestampType),
          StructField("boolean", BooleanType)))
        val emptyDataFrame = executionContext.dataFrameBuilder.buildDataFrame(
          schema,
          sparkContext.parallelize(Seq.empty[Row]))

        val report = emptyDataFrame.report(executionContext)

        val tables = report.content.tables
        val dataSampleTable = tables.get(DataFrameReportGenerator.DataSampleTableName).get
        dataSampleTable.columnNames shouldBe
          Some(List("string", "numeric", "categorical", "timestamp", "boolean"))
        dataSampleTable.rowNames shouldBe None
        dataSampleTable.values shouldBe List.empty
        testDataFrameSizeTable(tables, 5, 0)
      }
      "DataFrame consists of null values only" in {
        val schema = StructType(Seq(
          StructField("string", StringType),
          StructField("numeric", DoubleType),
          StructField("categorical", IntegerType),
          StructField("timestamp", TimestampType),
          StructField("boolean", BooleanType)))
        val emptyDataFrame = executionContext.dataFrameBuilder.buildDataFrame(
          schema,
          sparkContext.parallelize(Seq(
            Row(null, null, null, null, null),
            Row(null, null, null, null, null),
            Row(null, null, null, null, null))))

        val report = emptyDataFrame.report(executionContext)

        val tables = report.content.tables
        val dataSampleTable = tables.get(DataFrameReportGenerator.DataSampleTableName).get
        dataSampleTable.columnNames shouldBe
          Some(List("string", "numeric", "categorical", "timestamp", "boolean"))
        dataSampleTable.rowNames shouldBe None
        dataSampleTable.values shouldBe List(
          List(None, None, None, None, None),
          List(None, None, None, None, None),
          List(None, None, None, None, None))
        testDataFrameSizeTable(tables, 5, 3)
      }
    }
    "shorten long string values in sample data and in distribution tables" in {
      val schema = StructType(Seq(StructField("string", StringType)))

      val longValuePrefix = "A" * DataFrameReportGenerator.StringPreviewMaxLength

      val first = "AAA"
      val second = longValuePrefix + "B"
      val third = longValuePrefix + "C"

      val data = Seq(
        first,
        second,
        third
      ).map(v => Row(v))

      val dataFrame = executionContext.dataFrameBuilder.buildDataFrame(
        schema,
        sparkContext.parallelize(data)
      )

      val report = dataFrame.report(executionContext)

      val shortened = longValuePrefix + "..."

      val sampleTable = report.content.tables(DataFrameReportGenerator.DataSampleTableName)
      tableContains(0, sampleTable, first)
      tableContains(0, sampleTable, shortened)
      sampleTable.values.size shouldBe 3

      val buckets = report.content.distributions("string")
        .asInstanceOf[DiscreteDistribution].categories

      buckets shouldBe Seq(first, shortened, shortened)
    }
  }

  def dataWithColumnsCountOverThreshold(): DataFrame = {
    val fieldNames = for {
      i <- 1 to DataFrameReportGenerator.ColumnNumberToGenerateSimplerReportThreshold + 1
    } yield s"field$i"

    val fields = fieldNames.map(s => StructField(s, StringType))
    val schema = StructType(fields)
    val row = Row(fields.map(_.name): _*)

    executionContext.dataFrameBuilder.buildDataFrame(
      schema,
      sparkContext.parallelize(Seq(row)))
  }

  private def tableContains(column: Int, table: Table, value: String) = {
    table.values.map(_.apply(column)) should contain(Some(value))
  }

  private def testReportTables(
      cellValue: Option[String],
      columnNameBase: String,
      dataFrameColumnsNumber: Int,
      dataFrameRowsNumber: Int): Registration = {
    val dataFrame = executionContext.dataFrameBuilder.buildDataFrame(
      buildSchema(dataFrameColumnsNumber, columnNameBase),
      buildRDDWithStringValues(dataFrameColumnsNumber, dataFrameRowsNumber, cellValue))

    val report = dataFrame.report(executionContext)

    val tables: Map[String, Table] = report.content.tables
    testDataSampleTable(
      cellValue,
      columnNameBase,
      dataFrameColumnsNumber,
      dataFrameRowsNumber,
      tables)
    testDataFrameSizeTable(tables, dataFrameColumnsNumber, dataFrameRowsNumber)
  }

  private def testDataSampleTable(
      cellValue: Option[String],
      columnNameBase: String,
      dataFrameColumnsNumber: Int,
      dataFrameRowsNumber: Int,
      tables: Map[String, Table]): Registration = {
    val dataSampleTable = tables.get(DataFrameReportGenerator.DataSampleTableName).get
    val expectedRowsNumber: Int =
      Math.min(DataFrameReportGenerator.MaxRowsNumberInReport, dataFrameRowsNumber)
    dataSampleTable.columnNames shouldBe
      Some((0 until dataFrameColumnsNumber).map(columnNameBase + _))
    dataSampleTable.rowNames shouldBe None
    dataSampleTable.values shouldBe
      List.fill(expectedRowsNumber)(List.fill(dataFrameColumnsNumber)(cellValue))
  }

  private def testDataFrameSizeTable(
      tables: Map[String, Table],
      numberOfColumns: Int,
      numberOfRows: Long): Registration = {
    val dataFrameSizeTable = tables.get(DataFrameReportGenerator.DataFrameSizeTableName).get
    dataFrameSizeTable.columnNames shouldBe Some(List("Number of columns", "Number of rows"))
    dataFrameSizeTable.rowNames shouldBe None
    dataFrameSizeTable.values shouldBe
      List(List(Some(numberOfColumns.toString), Some(numberOfRows.toString)))
  }

  private def buildSchema(numberOfColumns: Int, columnNameBase: String): StructType = {
    StructType((0 until numberOfColumns).map(i => StructField(columnNameBase + i, StringType)))
  }

  private def buildRDDWithStringValues(
      numberOfColumns: Int,
      numberOfRows: Int,
      value: Option[String]): RDD[Row] =
    sparkContext.parallelize(
      List.fill(numberOfRows)(Row(List.fill(numberOfColumns)(value.orNull): _*)))
}
