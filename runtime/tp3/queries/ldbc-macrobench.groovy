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
oid =  g.V().has('xlabel', 'person').values('oid').max().next()
exec_time = System.nanoTime() - t;
result_row = [ DATABASE, DATASET, QUERY + "-max-oid", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), oid];
println result_row.join(',');


//println oid

//select max iid "person:933",
t = System.nanoTime();
iid =  g.V().has('xlabel', 'person').values('iid').order().by(decr).limit(1).next()
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY + "-max-iid", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), iid];
println result_row.join(',');


newIid = (iid.split(':')[1] as Integer) + 1
newOid = (oid + 1)


//------- User Insertion -----
t = System.nanoTime();
new_v = g.addV().property("firstName",firstNames[fNidx]).property("lastName",lastNames[lNidx]).property("creationDate",creationDates[cDidx]).property("birthday",birthdays[bDidx]).property("locationIP",locationIPs[iPidx]).property("email",firstNames[fNidx]+"."+lastNames[lNidx]+SID+"@mymail.com").property("gender",sex).property("browserUsed","Opera").property("xlabel","person").property("language","si").property("oid", newOid).property("iid", "person:" + newIid).next()

//g.addV().property(ID_FIELD_NAME, ID_VALUE).iterate();

if(!SKIP_COMMIT){ try { g.tx().commit();
    } catch (MissingMethodException e) { System.err.println("Failed g.tx().commit()."); }
}

exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY + "-create", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "{ ... }"];
println result_row.join(',');


//------- User Profile: Lives, Study, Works -----


t = System.nanoTime();
pCity = g.V().has('type', 'city').filter(has('xname',gt(placeNames[plNidx]))).barrier().order().by('xname', incr).limit(1).next()
new_v.addEdge("isLocatedIn",pCity);

if(!SKIP_COMMIT){ try { g.tx().commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.tx().commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;


result_row = [ DATABASE, DATASET, QUERY+ "-city", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "isLocatedIn " + pCity.value('xname')];
println result_row.join(',');



t = System.nanoTime();
pWork = g.V().has('type', 'company').filter(has('xname',gt(companyNames[cpNidx]))).barrier().order().by('xname', incr).limit(1).next()
new_v.addEdge("workAt",pWork);



if(!SKIP_COMMIT){ try { g.tx().commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.tx().commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-company", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "workAt " + pWork.value('xname')];
println result_row.join(',');


t = System.nanoTime();
pEdu = g.V().has('type', 'university').filter(has('xname',gt(placeNames[plNidx]))).barrier().order().by('xname', incr).limit(1).next()
new_v.addEdge("studyAt",pEdu);

if(!SKIP_COMMIT){ try { g.tx().commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.tx().commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-university", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "studyAt " + pEdu.value('xname')];
println result_row.join(',');


//------- User Connect  -----

t = System.nanoTime();
cc=0
friend1 = g.V().filter(has('firstName',gte(peopleNames[peNidx]))).barrier().order().by('firstName', incr).limit(1)
if(friend1.hasNext()){
  new_v.addEdge("knows", friend1.next());
  cc = 1
}

if(!SKIP_COMMIT){ try { g.tx().commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.tx().commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

// ex: -friend
result_row = [ DATABASE, DATASET, QUERY+"-friend1", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "friend1 " + cc ];
println result_row.join(',');



t = System.nanoTime();
cc=0
friend2 = g.V().filter(has('lastName',gte(peopleNames[peNidx]))).barrier().order().by('firstName', incr).limit(1)
if(friend2.hasNext()){
  new_v.addEdge("knows", friend2.next());
  cc = 1
}

if(!SKIP_COMMIT){ try { g.tx().commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.tx().commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

// ex: -friend
result_row = [ DATABASE, DATASET, QUERY+"-friend1", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "friend2 " +  cc];
println result_row.join(',');



t = System.nanoTime();
cc=0
friend3 = g.V().has("lastName", lastNames[lNidx]).filter(has("firstName", neq(firstNames[fNidx]))).barrier().order().by('firstName',decr).limit(1)
if(friend3.hasNext()){
  new_v.addEdge("knows", friend3.next());
  cc = 1
}

//g.addEdge(new_v,friend3,"knows");



if(!SKIP_COMMIT){ try { g.tx().commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.tx().commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;


// ex: friend-and
result_row = [ DATABASE, DATASET, QUERY+"-friend2", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "friend3 " + cc];
println result_row.join(',');





//------- Get friends tags  -----


t = System.nanoTime();
tags=[]
tt = g.V().has('oid', newOid).out('knows').out('hasInterest').barrier().order().by(inE('hasInterest').count(), decr
     ).limit(10).values("oid").fill(tags)
exec_time = System.nanoTime() - t;


result_row = [ DATABASE, DATASET, QUERY+"-friend-tags", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "top10-tags " + ( tags.size() > 0 ? tags[0] : 'nil')];
println result_row.join(',');


//------- Connect to friends tags  -----
t = System.nanoTime();
g.V().as('v').has("oid", within(tags)).as('all').sideEffect{
  new_v.addEdge("hasInterest", it.get());
  }.iterate()

//.addE("hasInterest").from(new_v).to('all').iterate()


if(!SKIP_COMMIT){ try { g.tx().commit();
    } catch (MissingMethodException e) { System.err.println("Does not support g.tx().commit(). Ignoring."); }
}
exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-add-tags", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), "add-tags " + tags.size()];
println result_row.join(',');


//------- LDBC Q1  -----

level=0
DEPTH=3

t = System.nanoTime();
llist = []

count =  g.V().has('oid', newOid).map{[ v:it.get(), l:0 ]}.repeat(
                map{it.get().v
                }.aggregate("x").out('knows').dedup().where(without("x"))
                .map{[ v:it.get(), l:it.loops()+1 ]}
              ).times(DEPTH).emit().filter{
                  it.get().v.value("firstName").equals(firstNames[f2Nidx])
              }.barrier().order().by(map{it.get().l}, incr
              ).limit(10).map{
                nn=it.get().v;
                [
                 oid: nn.value('oid'),
                 firstName: nn.value('firstName'),
                 lastName: nn.value('lastName'),
                 l: it.get().l,
                 location: nn.vertices(Direction.OUT, 'isLocatedIn').next().value('xname'),
                 birthday:nn.value('birthday'),
                 creationDate:nn.value('creationDate'),
                 study:nn.edges(Direction.OUT, 'studyAt').collect{
                    [ name:it.inVertex().value('xname'),
                      class:it.value('classYear'),
                      city:it.inVertex().vertices(Direction.OUT, 'isLocatedIn').next().value('xname')
                    ]
                  }
                ]
              }.fill(llist)


exec_time = System.nanoTime() - t;

// ex: q1
result_row = [ DATABASE, DATASET, QUERY+"-friend-of-friend", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),  llist.size()];
println result_row.join(',');



//------- Triangle Closure  -----

t = System.nanoTime();
nodes =[]
g.V().has('oid', newOid).aggregate('a').out('knows').aggregate('a').out('knows').where(without('a')
  ).barrier().order().by('lastName', incr).limit(10
  ).map{
    v= it.get();
    p = v.vertices(Direction.OUT, 'isLocatedIn');
    return [
  'oid': v.value('oid') ,
  'firstName': v.value('firstName') ,
  'lastName': v.value('lastName') ,
  'location': p.hasNext() ? p.next().value('xname') :"None",
  'birthday': v.value('birthday') ,
  'creationDate': v.value('creationDate')
    ]
  }.fill(nodes)

  // coalesce(project(
  // 'oid',
  // 'firstName',
  // 'lastName',
  // 'location',
  // 'birthday',
  // 'creationDate'
  // ).by('oid').by('firstName').by('lastName').by(coalesce(out('isLocatedIn').properties('xname').value(), constant("None"))).by('birthday').by('creationDate'),
  // constant({})
  // ).fill(nodes);


exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-triangle-closure", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),  nodes.size()];
println result_row.join(',');


//------- SEARCH PLACES -----

level=0
DEPTH=3

t = System.nanoTime();
visited = [] as Set;
places = []
REL_ARRAY=['isLocatedIn','studyAt', 'workAt']

g.V().has('oid', newOid).aggregate("x").repeat(
                    both('knows').where(without("x")).aggregate("x").dedup()
                  ).times(DEPTH).emit().out(*REL_ARRAY).choose(
                    has('type', 'city'),
                     identity(),
                     out('isLocatedIn')
                  ).values('xname').barrier().dedup().order().by(incr).limit(20).fill(places)


exec_time = System.nanoTime() - t;

result_row = [ DATABASE, DATASET, QUERY+"-places", String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),  places.size()];
println result_row.join(',');

//println String.valueOf(list)
