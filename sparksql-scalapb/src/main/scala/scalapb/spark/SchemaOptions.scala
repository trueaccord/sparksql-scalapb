package scalapb.spark

import scalapb.descriptors.FieldDescriptor
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion
import com.google.protobuf.Field
import org.apache.parquet.example.data.simple.Primitive
import org.apache.arrow.flatbuf.Schema
import scalapb.descriptors.Descriptor
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.types.DoubleType
import org.apache.spark.sql.types.BooleanType
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.LongType
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.FloatType
import org.apache.spark.sql.types.BinaryType

case class SchemaOptions(
    columnNaming: ColumnNaming,
    retainPrimitiveWrappers: Boolean
) {
  def withScalaNames = copy(columnNaming = ColumnNaming.ScalaNames)

  def withProtoNames = copy(columnNaming = ColumnNaming.ProtoNames)

  def withRetainedPrimitiveWrappers = copy(retainPrimitiveWrappers = true)

  private[scalapb] def customDataTypeFor(message: Descriptor): Option[DataType] =
    if (retainPrimitiveWrappers) None
    else SchemaOptions.PrimitiveWrapperTypes.get(message)

  private[scalapb] def isUnpackedPrimitiveWrapper(message: Descriptor) =
    !retainPrimitiveWrappers && SchemaOptions.PrimitiveWrapperTypes.contains(message)
}

object SchemaOptions {
  val Default = SchemaOptions(ColumnNaming.ProtoNames, retainPrimitiveWrappers = false)

  def apply(): SchemaOptions = Default

  private def buildWrapper[T <: GeneratedMessage](implicit
      cmp: GeneratedMessageCompanion[T]
  ) = {
    cmp.scalaDescriptor -> ProtoSQL.dataTypeFor(
      cmp.scalaDescriptor.fields.find(_.name == "value").get
    )
  }

  private[scalapb] val PrimitiveWrapperTypes = Seq(
    com.google.protobuf.wrappers.DoubleValue.scalaDescriptor -> DoubleType,
    com.google.protobuf.wrappers.BoolValue.scalaDescriptor -> BooleanType,
    com.google.protobuf.wrappers.BytesValue.scalaDescriptor -> BinaryType,
    com.google.protobuf.wrappers.Int32Value.scalaDescriptor -> IntegerType,
    com.google.protobuf.wrappers.Int64Value.scalaDescriptor -> LongType,
    com.google.protobuf.wrappers.StringValue.scalaDescriptor -> StringType,
    com.google.protobuf.wrappers.FloatValue.scalaDescriptor -> FloatType,
    com.google.protobuf.wrappers.UInt32Value.scalaDescriptor -> IntegerType,
    com.google.protobuf.wrappers.UInt64Value.scalaDescriptor -> LongType
  ).toMap
}

abstract class ColumnNaming {
  def fieldName(fd: FieldDescriptor): String
}

object ColumnNaming {
  case object ProtoNames extends ColumnNaming {
    def fieldName(fd: FieldDescriptor): String = fd.name
  }

  case object ScalaNames extends ColumnNaming {
    override def fieldName(fd: FieldDescriptor): String = fd.scalaName
  }
}
