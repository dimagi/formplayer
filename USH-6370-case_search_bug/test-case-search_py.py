import json
import random
import time
from collections import Counter
from http.cookies import SimpleCookie

import requests

ITERATIONS = 400
COMMCARE_URL = 'https://staging.commcarehq.org'
EXPECTED_CASE_TYPE = 'capacity'
CASE_TYPE_INDEX = 0  # In entities.data

def is_bad(entity):
    return entity['data'][CASE_TYPE_INDEX] != EXPECTED_CASE_TYPE

# Open the network tab in your browser console, then go load the case search page
# Search for "navigate_menu" to find the actual request.  Click on it
# Click on the "Headers" tab for this request, then scroll down to "Cookie"
# Copy the _value_ of the cookie header and paste inside the """s
# It should be a list of key/value pairs separated by semicolons

COOKIE_STRING = """
ko-pagination-web-users=5; __stripe_mid=e96e2a02-85bf-4519-9b1d-daac92f1f0b196cea7; ko-pagination-web-user-invitations=5; ko-pagination-releases-table=5; hubspotutk=55ba57106ea86230817696ba028330ba; __hs_cookie_cat_pref=1:true,2:true,3:true; ko-pagination-release-logs-table=5; ko-pagination-registry-audit-logs=5; ko-pagination-export-list=5; __hssrc=1; demo_workflow_mar2019_ab=control; viewed_domain_alerts="[]"; formplayer_session=257abf38236f5687c937fae96f10b458; lang=en; _gid=GA1.2.1938428975.1776105850; csrftoken=AA5hScRpo5Kwy1mFD7eUJ08IjvtplDa3; sessionid=q9u6rkan80vynko94cjhcqy0dzq30u5n; __hstc=187943799.55ba57106ea86230817696ba028330ba.1762280662206.1776105849967.1776182624314.34; _gcl_au=1.1.1700321841.1770649614.1777165924.1776182680.1776182679; _ga_J0DLWD6K5X=GS2.1.s1776182625$o35$g1$t1776182679$j6$l0$h0; _ga_7H9XDCY5RL=GS2.1.s1776182588$o115$g1$t1776182852$j29$l0$h0; _ga_W5RB3F1N34=GS2.1.s1776193447$o127$g1$t1776193458$j49$l0$h0; _ga=GA1.1.1428754424.1760370579; JSESSIONID=BC1D65F304FB00A8CA5A7B8B65C6E612; XSRF-TOKEN=18925193-8ace-4aee-bde5-a428e76046af; _ga_LQ2TV8L6EJ=GS2.1.s1776193447$o71$g1$t1776193503$j4$l0$h0
"""
# Firefox: Go to the "Request" tab, click the "Raw" toggle, and copy the request payload here:
# Chrome: Go to the "Payload" tab, click "View source", and copy the request payload here:
CASE_LIST_PAYLOAD = json.loads("""
    {"username":"mriese@dimagi.com","restoreAs":null,"domain":"co-carecoordination-test","app_id":"f49bfa2f43854db4ab124e9dcdba1978","locale":"en","selections":["0"],"offset":0,"search_text":null,"query_data":{},"cases_per_page":5,"preview":false,"tz_offset_millis":-21600000,"tz_from_browser":"America/Chicago","windowWidth":"1346","keepAPMTraces":false}
    """)

# Next, click on one of the results in the case list to make another request.
# Copy the payload from that request here:
INFORMATION_PAYLOAD = json.loads("""
    {"username":"mriese@dimagi.com","restoreAs":null,"domain":"co-carecoordination-test","app_id":"f49bfa2f43854db4ab124e9dcdba1978","locale":"en","selections":["0","144f496e-185f-42f2-a689-428dcf864493"],"offset":0,"search_text":null,"query_data":{},"cases_per_page":10,"preview":false,"tz_offset_millis":-21600000,"tz_from_browser":"America/Chicago","windowWidth":"1346","keepAPMTraces":false}
    """)
# Now you can run this script with `$ python test-case-search.py`.
# Feel free to play around with the page size as well - that's encoded in the payloads above.
# This might take more than 50 iterations to get a hit, but this procedure has worked for me many times

#############################################################################################

_c = SimpleCookie()
_c.load(COOKIE_STRING)
cookies = {k: v.value for k, v in _c.items()}


headers = {
    'User-Agent': 'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:146.0) Gecko/20100101 Firefox/146.0',
    'Accept': 'application/json, text/javascript, */*; q=0.01',
    'Accept-Language': 'en-US,en;q=0.7,es;q=0.3',
    'Origin': COMMCARE_URL,
    'DNT': '1',
    'Sec-GPC': '1',
    'X-XSRF-TOKEN': cookies['XSRF-TOKEN'],
    'Content-Type': 'application/json;charset=UTF-8',
    'Cache-Control': 'no-cache',
    'Pragma': 'no-cache',
}


def make_request(session, payload):
    global cookie, headers

    url = f"{COMMCARE_URL}/formplayer/navigate_menu"
    response = session.post(url, headers=headers, json=payload, cookies=cookies)
    response.raise_for_status()
    cookies['jsessionid'] = response.cookies['JSESSIONID']
    cookies['xsrf_token'] = response.cookies['XSRF-TOKEN']
    headers['X-XSRF-TOKEN'] = response.cookies['XSRF-TOKEN']
    return response.json()


def run_test():
    with requests.Session() as session:
        bad_queries = 0
        first_response = None
        for i in range(1, ITERATIONS + 1):
            data = make_request(session, CASE_LIST_PAYLOAD)

            print(i, end=' ')
            case_types = []
            bad_query = False
            for entity in data['entities']:
                bad = is_bad(entity)
                bad_query |= bad
                print('💥' if bad else '✅', end='')
                case_types.append(entity['data'][CASE_TYPE_INDEX])

            print('')
            print('    ', Counter(case_types))

            if bad_query:
                bad_queries += 1
                print(f"\n💥 Hit the bug on iteration {i}. Stopping.")
                break

            case_id = random.choice([e['id'] for e in data['entities']])
            INFORMATION_PAYLOAD['selections'] = ["0", case_id]
            print(f"    'clicking' {INFORMATION_PAYLOAD['selections']}")
            data = make_request(session, INFORMATION_PAYLOAD)
            time.sleep(1)
    print(f"\nRun Summary: {bad_queries}/{ITERATIONS} queries had bad results")


if __name__ == "__main__":
    run_test()
