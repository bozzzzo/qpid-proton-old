#ifndef PROTON_TYPES_H
#define PROTON_TYPES_H 1

/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

#include <sys/types.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef int32_t  pn_sequence_t;
typedef uint32_t pn_millis_t;
typedef uint32_t pn_seconds_t;
typedef uint64_t pn_timestamp_t;
typedef uint32_t pn_char_t;
typedef uint32_t pn_decimal32_t;
typedef uint64_t pn_decimal64_t;
typedef struct {
  char bytes[16];
} pn_decimal128_t;
typedef struct {
  char bytes[16];
} pn_uuid_t;

typedef struct {
  size_t size;
  char *start;
} pn_bytes_t;

pn_bytes_t pn_bytes(size_t size, char *start);
pn_bytes_t pn_bytes_dup(size_t size, const char *start);

#ifdef __cplusplus
}
#endif

#endif /* types.h */
