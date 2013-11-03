/*
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

#include "proton/message.h"
#include "proton/messenger.h"

#include "pncompat/misc_funcs.inc"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#define check(messenger)                                                     \
  {                                                                          \
    if(pn_messenger_errno(messenger))                                        \
    {                                                                        \
      die(__FILE__, __LINE__, pn_error_text(pn_messenger_error(messenger))); \
    }                                                                        \
  }                                                                          \

void die(const char *file, int line, const char *message)
{
  fprintf(stderr, "%s:%i: %s\n", file, line, message);
  exit(1);
}

void usage(const char *address)
{
  printf("Usage: send [-a addr] [message]\n");
  printf("-a     \tThe target address [amqp[s]://domain[/name]] (%s)\n", address);
  printf("-n     \tThe messenger name (default uuid)\n");
  printf("-r     \trequest reply and expect it (can be specified more than once)\n");
  printf("-w     \tuncleanly exit before last reply is expected (can be specified more than once)\n");
  printf("message\tA text string to send.\n");
  exit(0);
}

int main(int argc, char** argv)
{
  int c;
  opterr = 0;
  int wait_reply = 0;
  int reply = 0;
  char * address = (char *) "amqp://0.0.0.0";
  char * msgtext = (char *) "Hello World!";
  char * name = NULL;

  while((c = getopt(argc, argv, "hrwa:n:")) != -1)
  {
    switch(c)
    {
    case 'h': usage(address); break;
    case 'r': reply++; wait_reply++; break;
    case 'w': wait_reply--; break;
    case 'a': address = optarg; break;
    case 'n': name = optarg; break;
    case '?':
      if(optopt == 'a')
      {
        fprintf(stderr, "Option -%c requires an argument.\n", optopt);
      }
      else if(isprint(optopt))
      {
        fprintf(stderr, "Unknown option `-%c'.\n", optopt);
      }
      else
      {
        fprintf(stderr, "Unknown option character `\\x%x'.\n", optopt);
      }
      return 1;
    default:
      abort();
    }
  }

  if (optind < argc) msgtext = argv[optind];

  pn_message_t * message;
  pn_messenger_t * messenger;

  message = pn_message();
  messenger = pn_messenger(name);

  pn_messenger_start(messenger);

  pn_message_set_address(message, address);
  if (reply)
    pn_message_set_reply_to(message, "~");
  pn_data_t *body = pn_message_body(message);
  pn_data_put_string(body, pn_bytes(strlen(msgtext), msgtext));
  pn_messenger_put(messenger, message);
  check(messenger);
  pn_messenger_send(messenger, -1);
  check(messenger);
  for (int i = 0; i < reply; i++) {
    if ( i < wait_reply) {
      printf("wait reply %d/%d/%d\n", i, wait_reply, reply);
      pn_messenger_recv(messenger, 1);
      check(messenger);
      pn_messenger_get(messenger, message);
      check(messenger);

      char buffer[1024];
      size_t buffsize = sizeof(buffer);
      pn_data_t *body = pn_message_body(message);
      pn_data_format(body, buffer, &buffsize);

      printf("Address: %s\n", pn_message_get_address(message));
      const char* subject = pn_message_get_subject(message);
      subject = subject ? subject : "(no subject)";
      printf("Subject: %s\n", subject);
      printf("Content: %s\n", buffer);
    } else if (reply) {
      printf("Unclean exit before reply %d/%d/%d\n", i, wait_reply, reply);
      exit(0);
    }
  }

  pn_messenger_stop(messenger);
  pn_messenger_free(messenger);
  pn_message_free(message);

  return 0;
}
