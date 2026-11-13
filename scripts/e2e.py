"""Real HTTP demonstration against a running development server. Uses only the stdlib."""
import json, os, time, urllib.request, urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
for line in (ROOT/'.env').read_text().splitlines():
 if line and not line.startswith('#') and '=' in line:
  k,v=line.split('=',1);os.environ.setdefault(k,v.strip('"'))
assert os.environ.get('SEED_DEMO')=='true','Run only against your seeded development database'
BASE=os.environ.get('E2E_URL','http://127.0.0.1:8080')+'/api'
results=[]
def call(path,token=None,body=None,method=None,expected=200):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 r=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method or ('POST' if body is not None else 'GET'))
 try:
  with urllib.request.urlopen(r,timeout=15) as response:
   code=response.status;raw=response.read()
 except urllib.error.HTTPError as e:code=e.code;raw=e.read()
 assert code==expected,(path,code,raw.decode()[:300])
 return json.loads(raw) if raw else None
def login(email):return call('/auth/login',body={'email':email,'password':os.environ['DEMO_PASSWORD']})
admin=login('admin@iitsm.local');employee=login('employee@iitsm.local');other=login('employee2@iitsm.local');techs=[login('technician@iitsm.local'),login('technician2@iitsm.local')]
a=admin['token'];e=employee['token'];catalog=call('/catalog',e);service=next(x['id'] for x in catalog if x['kind']=='SERVICE' and x['name']=='Network & connectivity');category=next(x['id'] for x in catalog if x['kind']=='CATEGORY')
def create(title,service_id=service):return call('/incidents',e,{'title':title,'description':'Development end-to-end demonstration with real API and MySQL persistence.','serviceId':service_id,'categoryId':category,'impact':1,'urgency':1,'criticality':1,'affectedUsers':1},expected=201)
def technician(i):return next(x['token'] for x in techs if x['user']['id']==i['technicianId'])
def action(i,token,name,text=''):return call('/incidents/'+str(i['id'])+'/actions',token,{'action':name,'text':text})
i=create('Demo · Standard incident resolution');t=technician(i)
assert i['assignments'][-1]['selectionSource']=='FALLBACK'
assert len(i['sla'])==2 and all(c['startedAt']==i['createdAt'] for c in i['sla'])
call('/incidents/'+str(i['id']),other['token'],expected=403)
action(i,t,'ACKNOWLEDGE');action(i,t,'START');action(i,t,'REQUEST_INFO','Please provide the error code');action(i,e,'RESPOND','The error code is VPN-502');action(i,t,'RESOLVE','Corrected the VPN profile and verified connectivity');i=action(i,e,'VERIFY');assert i['status']=='CLOSED'
i=call('/incidents/'+str(i['id'])+'/feedback',e,{'rating':5,'comment':'Verified demo workflow'});assert len(i['feedback'])==1
results.append({'workflow':'Standard resolution, waiting/resume, verification, feedback, private access','status':'PASS','incident':i['reference']})
response=i['sla'][0];initial=i['sla'][1];i=action(i,e,'REOPEN','Connectivity failed again');assert len(i['sla'])==3;assert i['sla'][0]==response;assert i['sla'][1]==initial;assert i['sla'][2]['stoppedAt'] is None
results.append({'workflow':'Reopening preserves response and original resolution cycle','status':'PASS','incident':i['reference']})
action(i,t,'START');action(i,t,'RESOLVE','Replaced the faulty network adapter');action(i,e,'VERIFY')
existing=next((x for x in catalog if x['kind']=='SERVICE' and x['name']=='Demonstration SLA'),None)
svc=existing or call('/admin/catalog',a,{'kind':'SERVICE','name':'Demonstration SLA','active':True})
call('/admin/sla-policies',a,{'serviceId':svc['id'],'priority':'LOW','responseSeconds':4,'resolutionSeconds':120,'active':True})
j=create('Demo · Response SLA warnings and breach',svc['id']);deadline=time.monotonic()+45
while time.monotonic()<deadline:
 j=call('/incidents/'+str(j['id']),e)
 if any(s['threshold']==100 for s in j['sla'][0]['signals']):break
 time.sleep(2)
assert {s['threshold'] for s in j['sla'][0]['signals']}=={50,80,100}
assert not any(s['threshold']==100 for s in j['sla'][1]['signals'])
assert j['sla'][1]['stoppedAt'] is None
assert len({s['id'] for s in j['sla'][0]['signals']})==3
results.append({'workflow':'Scheduled response 50%/80% warnings and breach; independent resolution SLA','status':'PASS','incident':j['reference']})
assert call('/admin/sla',a);assert call('/admin/audit',a);assert call('/notifications',a);assert call('/reports',a)['total']>=2
call('/admin/users',e,expected=403)
results.append({'workflow':'Administrator monitoring, reports, audit, notifications and role enforcement','status':'PASS'})
ROOT.joinpath('docs/e2e-results.json').write_text(json.dumps({'executedAt':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),'database':'MySQL 8.4.4','results':results},indent=2))
for r in results:print(r['status']+' — '+r['workflow'])
