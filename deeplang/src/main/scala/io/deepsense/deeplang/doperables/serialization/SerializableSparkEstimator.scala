/**
 * Copyright 2016, deepsense.io
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


package io.deepsense.deeplang.doperables.serialization

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.{MLWritable, MLWriter}
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType

class SerializableSparkEstimator[T <: Model[T]](e: ml.Estimator[T])
  extends Estimator[T]
  with MLWritable {


  override val uid: String = "e2a121fe-da6e-4ef2-9c5e-56ee558c14f0"

  override def fit(dataset: DataFrame): T = {
    val result = e.fit(dataset)
    result match {
      case w: MLWritable => result
      case _ => new SerializableSparkModel[T](result).asInstanceOf[T]
    }
  }

  override def copy(extra: ParamMap): Estimator[T] =
    new SerializableSparkEstimator[T](e.copy(extra))

  override def write: MLWriter = new DefaultMLWriter(this)

  override def transformSchema(schema: StructType): StructType = e.transformSchema(schema)
}