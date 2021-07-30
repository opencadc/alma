#!/bin/bash

update-ca-certificates

exec "${@}"
