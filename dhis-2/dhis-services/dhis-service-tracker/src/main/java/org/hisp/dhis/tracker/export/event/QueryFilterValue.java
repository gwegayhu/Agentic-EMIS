/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.tracker.export.event;

import java.sql.Types;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.springframework.jdbc.core.SqlParameterValue;

@Value
public class QueryFilterValue {
  QueryOperator operator;

  /** Unary operators have no value. */
  @Nullable SqlParameterValue value;

  @SuppressWarnings("unchecked")
  public static QueryFilterValue of(
      QueryFilter filter, ValueTypedDimensionalItemObject valueTypeObject) {
    if (filter.getOperator().isUnary()) {
      return new QueryFilterValue(filter.getOperator(), null);
    }

    List<String> values =
        filter.getOperator().isIn()
            ? List.of(filter.getFilter().split(QueryFilter.OPTION_SEP))
            : List.of(filter.getFilter());

    ValueType.SqlType<Object> sqlType =
        (ValueType.SqlType<Object>) ValueType.JAVA_TO_SQL_TYPES.get(String.class);
    boolean needsConversion =
        filter.getOperator().isCastOperand() && valueTypeObject.getValueType().isNumeric();
    if (needsConversion) {
      sqlType =
          (ValueType.SqlType<Object>)
              ValueType.JAVA_TO_SQL_TYPES.get(valueTypeObject.getValueType().getJavaClass());
    }
    Object convertedValue = convertValues(sqlType, valueTypeObject, values);

    return new QueryFilterValue(
        filter.getOperator(),
        new SqlParameterValue(
            filter.getOperator().isIn() ? Types.ARRAY : sqlType.type(),
            filter.getOperator().isIn() ? convertedValue : ((List<?>) convertedValue).get(0)));
  }

  private static Object convertValues(
      ValueType.SqlType<Object> sqlType,
      ValueTypedDimensionalItemObject valueTypeObject,
      List<String> values) {
    try {
      return values.stream()
          .map(
              value -> {
                try {
                  return sqlType.producer().apply(value);
                } catch (NumberFormatException e) {
                  throw new IllegalArgumentException(
                      String.format(
                          "Filter for %s %s is invalid. The %s value type is numeric but the value `%s` is not.",
                          valueTypeObject
                                  instanceof org.hisp.dhis.trackedentity.TrackedEntityAttribute
                              ? "attribute"
                              : "data element",
                          valueTypeObject.getUid(),
                          valueTypeObject
                                  instanceof org.hisp.dhis.trackedentity.TrackedEntityAttribute
                              ? "attribute"
                              : "data element",
                          value));
                }
              })
          .toList();
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(
              "Filter for %s %s is invalid. The %s value type is numeric but the value `%s` is not.",
              valueTypeObject instanceof org.hisp.dhis.trackedentity.TrackedEntityAttribute
                  ? "attribute"
                  : "data element",
              valueTypeObject.getUid(),
              valueTypeObject instanceof org.hisp.dhis.trackedentity.TrackedEntityAttribute
                  ? "attribute"
                  : "data element",
              String.join(QueryFilter.OPTION_SEP, values)));
    }
  }
}
