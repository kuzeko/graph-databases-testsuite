#META:SID=[0-10]

SID = System.env.get("SID").toInteger();
ORDER_j = 0

// i=SID,
// ORDER_j=0,
// DATABASE
// DATASET
// QUERY
// ITERATION
// SID
// id_field_name =uid_field,
// id_field_value =MAX_UID


// create index
// name
// lastName
// firstName
// type


firstNames = [  "Abdul Hameed", "Adriaen", "Andrei", "Bob", "Cesar", "Chipo", "Chris", "David", "Ganesh", "Hans", "Isabel", "Jacques", "Jie",  "Jun", "Li", "Luis", "Mahinda", "Masahiro", "Michael", "Paul", "Paulo", "Poul", "Ruby", "Svetlana", "Wei", "Yang", "Zsolt"
]

fNidx =  (SID*37)%firstNames.size()
f2Nidx =  ((SID+1)*59)%firstNames.size()



lastNames = [ "Becker", "Bologan", "Bombo", "Choi", "Chung", "Ferrer", "Garcia", "Gonzales", "Hoffmann", "Hosseini", "Ito", "Jong", "Kiss", "Lei", "Liu", "Makarova", "Malik", "Nascimento", "Nielsen", "Perera", "Reyes", "Sato", "Thapa", "Wilson", "Zhang", "Zheng"
]
lNidx = (SID*113)%lastNames.size()

creationDates = [ "1363530810802", "1389100611989", "1389190711064", "1408930078066", "1408930079066", "1408930080066", "1408930081066", "1408930082066", "1408930083066", "1412842526053", "1412842527053", "1412842528053", "1412842529053"
]
cDidx = (SID*157)%creationDates.size()


birthdays = [ "321408000000",, "322444800000", "324691200000", "325123200000", "326764800000", "326851200000", "327542400000", "328492800000", "330566400000", "331862400000", "332294400000", "333504000000", "334540800000"
]
bDidx = (SID*179)%creationDates.size()


locationIPs = [ "1.1.55.15",  "103.22.202.182",  "114.134.175.114",  "121.54.81.151",  "178.249.113.42",  "181.52.248.99",  "186.185.177.193",  "186.19.49.99",  "188.140.76.78",  "192.248.2.123",  "193.164.232.172",  "196.202.254.176",  "200.10.162.126"
]
iPidx =  (SID*211)%creationDates.size()

sex = SID%2 == 0 ? "male" : "female"


placeNames = [ "Al", "Ba", "Be", "Ch", "De", "Dh", "Gu", "Is", "Ka", "La", "Li", "Mo", "Mu", "Na", "Sh", "Sa", "Ti", "To", "Zh"
]
plNidx =  (SID*223)%placeNames.size()


companyNames = [ "Ca", "Ch", "Co", "Da", "Ey", "Fa", "Go", "Gr", "Ha", "Li", "Ma", "Nu", "Pa", "Pe", "Pl", "Po", "Pr", "Th", "Ti", "Wa", "We", "Wo", "Ye"
]
cpNidx =  (SID*227)%companyNames.size()


peopleNames = [ "Al", "Av", "Be", "Ch", "Da", "El", "Em", "Et", "Is", "Ja", "Li", "Ma", "Mi", "No", "Ol", "So", "Wi"
]
peNidx =  (SID*251)%peopleNames.size()


//select max oid
t = System.nanoTime();
oid =  g.V('xlabel', 'person').max{it.oid}.getProperty('oid')
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY + "-max-oid", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), oid];
println result_row.join(',');


//println oid

//select max iid "person:933",
t = System.nanoTime();
iid =  g.V('xlabel', 'person').max{it.iid}.getProperty('iid')
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY + "-max-iid", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), iid];
println result_row.join(',');


newIid = (iid.split(':')[1] as Integer) + 1
newOid = (oid + 1)




property_map = [
"firstName":firstNames[fNidx],
"lastName":lastNames[lNidx],
"creationDate":creationDates[cDidx],
"birthday":birthdays[bDidx],
"locationIP":locationIPs[iPidx],
"email":firstNames[fNidx]+"."+lastNames[lNidx]+SID+"@mymail.com",
"gender":sex,
"browserUsed":"Opera",
"xlabel":"person",
"language":"si",
"oid": newOid,
"iid": "person:" + newIid
]

//------- User Insertion -----
t = System.nanoTime();
new_v = g.addVertex(null,property_map);

if(!SKIP_COMMIT){ try { g.commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.commit(). Ignoring."); }
}

exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY + "-create", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), String.valueOf(property_map)];
println result_row.join(',');


//------- User Profile: Lives, Study, Works -----

t = System.nanoTime();
pCity = g.V('type', 'city').has('xname', T.gte, placeNames[plNidx]).order{it.a.xname <=> it.b.xname}.next()
g.addEdge(new_v,pCity,"isLocatedIn");

if(!SKIP_COMMIT){ try { g.commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+ "-city", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "isLocatedIn " + pCity.getProperty('xname')];
println result_row.join(',');



t = System.nanoTime();
pWork = g.V('type', 'company').has('xname', T.gte, companyNames[cpNidx]).order{it.a.xname <=> it.b.xname}.next()
g.addEdge(new_v,pWork,"workAt");

if(!SKIP_COMMIT){ try { g.commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-company", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "workAt " + pWork.getProperty('xname')];
println result_row.join(',');


t = System.nanoTime();
pEdu = g.V('type', 'university').has('xname', T.gte, placeNames[plNidx]).order{it.a.xname <=> it.b.xname}.next()
g.addEdge(new_v,pEdu,"studyAt");

if(!SKIP_COMMIT){ try { g.commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-university", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "studyAt " + pEdu.getProperty('xname')];
println result_row.join(',');


//------- User Connect  -----

t = System.nanoTime();
friend1= []
g.V().has("firstName", T.gte, peopleNames[peNidx]).order{it.a.firstName <=> it.b.firstName}[0..<1].sideEffect{g.addEdge(new_v,it,"knows")}.fill(friend1)
//g.addEdge(new_v,friend1,"knows");

if(!SKIP_COMMIT){ try { g.commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-friend", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "friend1 " + friend1.size()];
println result_row.join(',');


t = System.nanoTime();
friend2 = []
g.V().has("lastName", T.gte, peopleNames[peNidx]).order{it.a.firstName <=> it.b.firstName}[0..<1].sideEffect{g.addEdge(new_v,it,"knows")}.fill(friend2)
//g.addEdge(new_v,friend2,"knows");

if(!SKIP_COMMIT){ try { g.commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-friend", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "friend2 " +   friend2.size()];
println result_row.join(',');

t = System.nanoTime();
friend3=[]
g.V("lastName", lastNames[lNidx]).hasNot("firstName",  firstNames[fNidx]).order{it.a.firstName <=> it.b.firstName}[0..<1].sideEffect{g.addEdge(new_v,it,"knows")}.fill(friend3)

//g.addEdge(new_v,friend3,"knows");



if(!SKIP_COMMIT){ try { g.commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-friend-and", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "friend3 " + friend3.size()];
println result_row.join(',');


//------- Get friends tags  -----


t = System.nanoTime();
list=[]
tt = g.V().has('oid', newOid).out('knows').out('hasInterest').groupBy{it}{it.in('hasInterest')}{it.size()}.cap.orderMap(T.decr)[0..9].oid.fill(list)
exec_time = System.nanoTime() - t;


result_row = [ DATABASE, DATASET, QUERY+"-friend-tags", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "top10-tags " + ( list.size() > 0 ? list[0] : 'nil')];
println result_row.join(',');

//------- Connect to friends tags  -----

t = System.nanoTime();
g.V.has('oid',T.in, list ).sideEffect{g.addEdge(new_v,it,"hasInterest")}.iterate()

if(!SKIP_COMMIT){ try { g.commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-add-tags", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "add-tags " + list.size()];
println result_row.join(',');


//------- LDBC Q1  -----

level=0
DEPTH=3

t = System.nanoTime();
//v = g.V().has('oid', newOid)
//visited = [v] as Set;
visited = [] as Set;
list = []
count =  g.V().has('oid', newOid).transform{[v:it]}.as('x').transform{it.v}._().store(visited).out('knows').dedup().except(visited).transform{[v:it]}.loop('x'){
               it.object.l=(it.loops-1); return it.loops <= DEPTH}{it.object.v.firstName == firstNames[f2Nidx]
            }.order{it.a.l <=> it.b.l}[0..9].transform{
              [l: it.l, oid:it.v.oid,firstName:it.v.firstName,lastName:it.v.lastName,location:it.v.out('isLocatedIn').xname.next(), birthday:it.v.birthday, creationDate:it.v.creationDate, study:it.v.outE('studyAt').transform{[name:it.inV.xname.next(), class:it.classYear  , city:it.inV.out('isLocatedIn').xname.next()]}.gather{it}.next() ]
            }.fill(list);


exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-q1", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),  list.size()];
println result_row.join(',');

//println String.valueOf(list)

//------- Triangle Closure  -----

t = System.nanoTime();

g.V().has('oid', newOid).as('l0').out('knows').as('l1').out('knows').except('l0').except('l1').order{it.a.lastName <=> it.b.lastName}[0..9].transform{
              [oid:it.oid,firstName:it.firstName,lastName:it.lastName,location:it.out('isLocatedIn').xname.next(), birthday:it.birthday, creationDate:it.creationDate ]
            }.fill(list);

exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-triangle-closure", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),  list.size()];
println result_row.join(',');


//System.out.println(String.valueOf(list))



//------- SEARCH PLACES -----

level=0
DEPTH=3

t = System.nanoTime();
visited = [] as Set;
list = []
REL_ARRAY=['isLocatedIn','studyAt', 'workAt']
count = g.V().has('oid', newOid).store(visited).as('x').both('knows').except(visited).store(visited).dedup().loop('x'){
    it.loops <= DEPTH
}{true}.out(*REL_ARRAY).ifThenElse{it.type=='city'}{
    it
}{
    it.out('isLocatedIn')
}.dedup().order{it.a.xname <=> it.b.xname}[0..20].xname.fill(list);

exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-places", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),  list.size()];
println result_row.join(',');

//println String.valueOf(list)
