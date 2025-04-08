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

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Types;
import java.util.List;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.SqlParameterValue;

class QueryFilterValueTest extends TestBase {

  private TrackedEntityAttribute tea1;

  @BeforeEach
  void setUp() {
    tea1 = createTrackedEntityAttribute('a');
    tea1.setValueType(ValueType.TEXT);
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeNumber() {
    tea1.setValueType(ValueType.NUMBER);
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "42.5");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.NUMERIC, sqlParameterValue.getSqlType());
    assertEquals(new java.math.BigDecimal("42.5"), sqlParameterValue.getValue());
    assertEquals("=", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeNumberInValues() {
    tea1.setValueType(ValueType.NUMBER);
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "42.5;17.2;7");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.NUMERIC, sqlParameterValue.getSqlType());
    @SuppressWarnings("unchecked")
    List<Object> values = (List<Object>) sqlParameterValue.getValue();
    assertEquals(
        List.of(
            new java.math.BigDecimal("42.5"),
            new java.math.BigDecimal("17.2"),
            new java.math.BigDecimal("7")),
        values);
    assertEquals("in", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeInteger() {
    tea1.setValueType(ValueType.INTEGER);
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "42");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.INTEGER, sqlParameterValue.getSqlType());
    assertEquals(42, sqlParameterValue.getValue());
    assertEquals("=", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeIntegerInValues() {
    tea1.setValueType(ValueType.INTEGER);
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "42;17;7");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.INTEGER, sqlParameterValue.getSqlType());
    assertEquals(List.of(42, 17, 7), sqlParameterValue.getValue());
    assertEquals("in", filterValue.sqlOperator());
  }

  @Test
  void shouldFailIfValueTypeIntegerValueIsNotInteger() {
    tea1.setValueType(ValueType.INTEGER);
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "42.5");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> QueryFilterValue.of(filter, tea1));

    assertContains("Could not convert value `42.5` to value type INTEGER.", exception.getMessage());
  }

  @Test
  void shouldFailIfValueTypeIntegerInValueContainsNonInteger() {
    tea1.setValueType(ValueType.INTEGER);
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "42;17.5;7");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> QueryFilterValue.of(filter, tea1));

    assertContains(
        "Could not convert value `42;17.5;7` to value type INTEGER.", exception.getMessage());
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeText() {
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "summer day");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.VARCHAR, sqlParameterValue.getSqlType());
    assertEquals("summer day", sqlParameterValue.getValue());
    assertEquals("=", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeTextInValues() {
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "summer;winter;spring");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.VARCHAR, sqlParameterValue.getSqlType());
    assertEquals(List.of("summer", "winter", "spring"), sqlParameterValue.getValue());
    assertEquals("in", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeTextAndOperatorLike() {
    QueryFilter filter = new QueryFilter(QueryOperator.LIKE, "summer");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.VARCHAR, sqlParameterValue.getSqlType());
    assertEquals("%summer%", sqlParameterValue.getValue());
    assertEquals("like", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeTextAndOperatorSW() {
    QueryFilter filter = new QueryFilter(QueryOperator.SW, "summer");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.VARCHAR, sqlParameterValue.getSqlType());
    assertEquals("summer%", sqlParameterValue.getValue());
    assertEquals("like", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForValueTypeTextAndOperatorEW() {
    QueryFilter filter = new QueryFilter(QueryOperator.EW, "summer");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.VARCHAR, sqlParameterValue.getSqlType());
    assertEquals("%summer", sqlParameterValue.getValue());
    assertEquals("like", filterValue.sqlOperator());
  }

  @Test
  void shouldFailIfValueTypeNumberValueIsNotNumeric() {
    tea1.setValueType(ValueType.NUMBER);
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "not a number");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> QueryFilterValue.of(filter, tea1));

    assertContains(
        "Could not convert value `not a number` to value type NUMBER.", exception.getMessage());
  }

  @Test
  void shouldFailIfValueTypeNumberInValueContainsNonNumeric() {
    tea1.setValueType(ValueType.NUMBER);
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "42.5;not a number;7");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> QueryFilterValue.of(filter, tea1));

    assertContains(
        "Could not convert value `42.5;not a number;7` to value type NUMBER.",
        exception.getMessage());
  }

  @Test
  void shouldNotValidateUnaryOperatorAsItHasNoValue() {
    tea1.setValueType(ValueType.NUMBER);
    QueryFilter filter = new QueryFilter(QueryOperator.NNULL);

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertNull(sqlParameterValue);
    assertEquals("is not null", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForTextValue() {
    tea1.setValueType(ValueType.TEXT);
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "test");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.VARCHAR, sqlParameterValue.getSqlType());
    assertEquals("test", sqlParameterValue.getValue());
    assertEquals("=", filterValue.sqlOperator());
  }

  @Test
  void shouldCreateSqlParameterValueForTextInValues() {
    tea1.setValueType(ValueType.TEXT);
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "test1;test2;test3");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.VARCHAR, sqlParameterValue.getSqlType());
    @SuppressWarnings("unchecked")
    List<Object> values = (List<Object>) sqlParameterValue.getValue();
    assertEquals(List.of("test1", "test2", "test3"), values);
    assertEquals("in", filterValue.sqlOperator());
  }

  @Test
  void shouldFailIfNumericValueIsNotNumeric() {
    tea1.setValueType(ValueType.NUMBER);
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "not-a-number");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> QueryFilterValue.of(filter, tea1));

    assertContains(
        "Could not convert value `not-a-number` to value type NUMBER.", exception.getMessage());
  }

  @Test
  void shouldFailIfNumericInValueContainsNonNumeric() {
    tea1.setValueType(ValueType.NUMBER);
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "42.5;not-a-number;7");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> QueryFilterValue.of(filter, tea1));

    assertContains(
        "Could not convert value `42.5;not-a-number;7` to value type NUMBER.",
        exception.getMessage());
  }

  @Test
  void shouldNotValidateNonNumericValue() {
    tea1.setValueType(ValueType.TEXT);
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "not-a-number");

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertEquals(Types.VARCHAR, sqlParameterValue.getSqlType());
    assertEquals("not-a-number", sqlParameterValue.getValue());
    assertEquals("=", filterValue.sqlOperator());
  }

  @Test
  void shouldNotValidateUnaryOperator() {
    tea1.setValueType(ValueType.NUMBER);
    QueryFilter filter = new QueryFilter(QueryOperator.NNULL, null);

    QueryFilterValue filterValue = QueryFilterValue.of(filter, tea1);
    SqlParameterValue sqlParameterValue = filterValue.value();

    assertNull(sqlParameterValue);
    assertEquals("is not null", filterValue.sqlOperator());
  }

  @Test
  void testSqlOperatorForUnaryOperator() {
    DataElement de = new DataElement();
    de.setValueType(ValueType.TEXT);
    de.setUid("abc123");

    QueryFilter filter = new QueryFilter(QueryOperator.NNULL);
    QueryFilterValue value = QueryFilterValue.of(filter, de);

    assertEquals("is not null", value.sqlOperator());
    assertNull(value.value());
  }

  @Test
  void testSqlOperatorForBinaryOperator() {
    DataElement de = new DataElement();
    de.setValueType(ValueType.TEXT);
    de.setUid("abc123");

    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "test");
    QueryFilterValue value = QueryFilterValue.of(filter, de);

    assertEquals("=", value.sqlOperator());
  }

  @Test
  void testSqlOperatorForInOperator() {
    DataElement de = new DataElement();
    de.setValueType(ValueType.TEXT);
    de.setUid("abc123");

    QueryFilter filter = new QueryFilter(QueryOperator.IN, "test1;test2");
    QueryFilterValue value = QueryFilterValue.of(filter, de);

    assertEquals("in", value.sqlOperator());
  }
}
