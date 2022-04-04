#!/bin/bash

box $1

checkServiceWhitelistValue "service-with-filtering" '["10.1.1.1/32","10.1.1.2/32","10.1.1.3/32","10.1.2.1/32","10.1.2.3/32"]'
