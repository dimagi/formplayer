#!/usr/bin/env bash

# example usage
# ./scripts/archive_dbs.sh dbs +2d
# to archive all dbs in the dbs/ directory that haven't been accessed for more than 2 days


dir=${1}
atime=${2}

if [ $(echo $atime | cut -c1) != "+" ]
then
    echo >&2 "atime must start with +: ${atime}"
    exit 1
fi

test -d ${dir} || {
    echo >&2 "dir must be a directory: ${dir}"
    exit 1
}

while read line
do
    db="${line}"
    lock="${line}.lock"
    gz="${line}.gz"

    if [ -f ${lock} ]
    then
        echo >&2 "skipping due to ${lock}"
        continue
    fi

    touch ${lock}
    gzip ${db}
    rm ${lock}
done < <(find ${dir} -name '*.db' -type f -atime ${atime})
