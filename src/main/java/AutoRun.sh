#!/bin/bash

echo "----Start the Meta Server----"
java com.owen31302.quorumcloud.MetaServer &
echo "----Start all Backup Servers----"
java com.owen31302.quorumcloud.BackupServer 0 &
java com.owen31302.quorumcloud.BackupServer 1 &
java com.owen31302.quorumcloud.BackupServer 2 &
java com.owen31302.quorumcloud.BackupServer 3 &
java com.owen31302.quorumcloud.BackupServer 4 &
echo "----Start the Client----"
java com.owen31302.quorumcloud.Client &