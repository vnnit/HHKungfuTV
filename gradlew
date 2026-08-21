#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
#
# ... standard gradlew wrapper ...
#

APP_BASE_NAME=`basename "$0"`
APP_HOME=`cd "\`dirname \"$0\"\`" > /dev/null && pwd -P`

exec gradle "$@"
