--------- beginning of main
07-28 20:52:15.536 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 20:52:20.142 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 20:52:25.383 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 20:52:27.968 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 20:52:30.573 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 20:52:34.756 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 20:52:35.622 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 20:53:42.813 E/rust    (29302): git_wrapper: push: push: SSH could not write data: Failure while draining incoming flow; class=Ssh (23)
07-28 20:53:42.814 E/GitManager(29302): Can't push: push: SSH could not write data: Failure while draining incoming flow; class=Ssh (23)
07-28 20:53:42.814 E/GitManager(29302): java.lang.Exception: Can't push: push: SSH could not write data: Failure while draining incoming flow; class=Ssh (23)
07-28 20:53:42.814 E/GitManager(29302): 	at y8.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:68)
07-28 20:53:42.814 E/GitManager(29302): 	at y8.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 20:53:42.814 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 20:53:42.814 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.k(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 20:53:42.814 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:498)
07-28 20:53:42.814 E/GitManager(29302): 	at p52.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:12)
07-28 20:53:42.814 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 20:53:42.814 E/GitManager(29302): 	at ui2.A(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:27)
07-28 20:53:42.814 E/GitManager(29302): 	at s.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 20:53:42.814 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:33)
07-28 20:53:42.814 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 20:53:42.814 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 20:53:42.814 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 20:53:42.814 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 20:53:42.814 E/StorageManager(29302): Can't push: push: SSH could not write data: Failure while draining incoming flow; class=Ssh (23)
07-28 20:56:40.909 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 20:56:40.912 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 20:56:40.912 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 20:56:40.912 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 20:56:40.912 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 20:56:40.912 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 20:56:40.912 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 20:56:40.912 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 20:56:40.912 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 20:56:40.912 E/GitManager(29302): 	at o52.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 20:56:40.912 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 20:56:40.912 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 20:56:40.912 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 20:56:40.912 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 20:56:40.912 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 20:56:40.912 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:12:57.633 E/rust    (29302): git_wrapper: pull: fetch: SSH could not execute request: Timed out waiting on socket; class=Ssh (23)
07-28 21:12:57.634 E/GitManager(29302): Can't pull: fetch: SSH could not execute request: Timed out waiting on socket; class=Ssh (23)
07-28 21:12:57.634 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: SSH could not execute request: Timed out waiting on socket; class=Ssh (23)
07-28 21:12:57.634 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:12:57.634 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:12:57.634 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:12:57.634 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:12:57.634 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:12:57.634 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:12:57.634 E/GitManager(29302): 	at v52.l(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:84)
07-28 21:12:57.634 E/GitManager(29302): 	at ng0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:73)
07-28 21:12:57.634 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:12:57.634 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:12:57.634 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:12:57.634 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:12:57.634 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:12:57.634 E/StorageManager(29302): Can't pull: fetch: SSH could not execute request: Timed out waiting on socket; class=Ssh (23)
07-28 21:15:53.196 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:15:53.197 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:15:53.197 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:15:53.197 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:15:53.197 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:15:53.197 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:15:53.197 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:15:53.197 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:15:53.197 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:15:53.197 E/GitManager(29302): 	at o52.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:15:53.197 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:15:53.197 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:15:53.197 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:15:53.197 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:15:53.197 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:15:53.197 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:15:59.799 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:15:59.800 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:15:59.800 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:15:59.800 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:15:59.800 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:15:59.800 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:15:59.800 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:15:59.800 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:15:59.800 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:15:59.800 E/GitManager(29302): 	at o52.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:15:59.800 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:15:59.800 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:15:59.800 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:15:59.800 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:15:59.800 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:15:59.800 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:06.572 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:06.573 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:06.573 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:06.573 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:16:06.573 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:16:06.573 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:16:06.573 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:16:06.573 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:16:06.573 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:16:06.573 E/GitManager(29302): 	at o52.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:16:06.573 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:16:06.573 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:16:06.573 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:16:06.573 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:16:06.573 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:16:06.573 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:13.316 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:13.317 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:13.317 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:13.317 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:16:13.317 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:16:13.317 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:16:13.317 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:16:13.317 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:16:13.317 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:16:13.317 E/GitManager(29302): 	at o52.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:16:13.317 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:16:13.317 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:16:13.317 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:16:13.317 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:16:13.317 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:16:13.318 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:23.705 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:23.706 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:23.706 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:16:23.706 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:16:23.706 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:16:23.706 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:16:23.706 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:16:23.706 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:16:23.706 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:16:23.706 E/GitManager(29302): 	at v52.l(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:84)
07-28 21:16:23.706 E/GitManager(29302): 	at w90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:168)
07-28 21:16:23.706 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:16:23.706 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:16:23.706 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:16:23.706 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:16:23.706 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:16:23.706 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:19:24.431 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:25.387 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:26.745 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:31.252 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:33.144 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:38.729 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:40.279 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:45.477 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:48.347 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:19:51.713 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:00.305 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:03.404 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:04.012 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:05.114 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:05.739 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:09.733 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:10.945 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:19.672 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:21.728 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:26.038 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:28.510 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:30.620 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:33.635 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:36.016 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:46.596 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:46.627 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:46.651 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:47.935 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:49.012 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:50.661 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:51.591 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:53.044 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:20:53.856 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:01.484 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:01.507 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:05.055 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:05.996 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:06.568 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:09.571 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:21:09.572 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:21:09.572 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:21:09.572 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:21:09.572 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:21:09.572 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:21:09.572 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:21:09.572 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:21:09.572 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:21:09.572 E/GitManager(29302): 	at v52.l(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:84)
07-28 21:21:09.572 E/GitManager(29302): 	at w90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:168)
07-28 21:21:09.572 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:21:09.572 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:21:09.572 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:21:09.572 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:21:09.572 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:21:09.572 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:21:11.465 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:13.250 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:15.552 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:16.338 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:17.265 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:19.951 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:21:19.952 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:21:19.952 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:21:19.952 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:21:19.952 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:21:19.952 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:21:19.952 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:21:19.952 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:21:19.952 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:21:19.952 E/GitManager(29302): 	at v52.l(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:84)
07-28 21:21:19.952 E/GitManager(29302): 	at w90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:168)
07-28 21:21:19.952 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:21:19.952 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:21:19.952 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:21:19.952 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:21:19.952 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:21:19.952 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:21:21.389 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:22.704 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:25.966 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:26.637 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:27.795 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:30.877 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:37.780 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:37.818 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:37.841 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:44.516 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:44.543 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:51.572 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:51.601 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:51.631 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:21:51.654 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:22:06.711 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:22:06.712 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:22:06.712 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:22:06.712 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:22:06.712 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:22:06.712 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:22:06.712 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:22:06.712 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:22:06.712 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:22:06.712 E/GitManager(29302): 	at v52.l(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:84)
07-28 21:22:06.712 E/GitManager(29302): 	at w90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:168)
07-28 21:22:06.712 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:22:06.712 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:22:06.712 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:22:06.712 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:22:06.712 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:22:06.712 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:22:17.822 E/ImeBackDispatcher(29302): Ime callback not found. Ignoring unregisterReceivedCallback. callbackId: 182448214
07-28 21:22:18.943 E/rust    (29302): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:22:18.944 E/GitManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:22:18.944 E/GitManager(29302): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:22:18.944 E/GitManager(29302): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:22:18.944 E/GitManager(29302): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:22:18.944 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:22:18.944 E/GitManager(29302): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:22:18.944 E/GitManager(29302): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:22:18.944 E/GitManager(29302): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:22:18.944 E/GitManager(29302): 	at v52.l(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:84)
07-28 21:22:18.944 E/GitManager(29302): 	at w90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:168)
07-28 21:22:18.944 E/GitManager(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:22:18.944 E/GitManager(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:22:18.944 E/GitManager(29302): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:22:18.944 E/GitManager(29302): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:22:18.944 E/GitManager(29302): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:22:18.944 E/StorageManager(29302): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:23:04.235 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:23:06.036 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:23:38.072 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:23:40.707 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:23:43.905 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:23:49.118 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:18.915 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:20.527 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:25.170 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:31.839 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:40.163 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:43.383 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:46.898 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:49.779 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:51.139 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:56.689 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:24:59.343 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:13.093 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:14.321 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:32.838 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:36.847 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:39.130 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:41.148 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:41.888 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:44.888 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:48.330 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:49.552 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:51.432 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:52.309 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:56.980 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:58.125 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:25:59.060 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:26:01.317 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:26:02.342 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:26:04.266 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:26:09.519 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:26:10.245 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:26:15.826 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:26:17.230 E/SQLiteLog(29302): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
--------- beginning of crash
07-28 21:26:34.740 E/AndroidRuntime(29302): FATAL EXCEPTION: main
07-28 21:26:34.740 E/AndroidRuntime(29302): Process: io.github.leonhardweiler.gitnote.nightly, PID: 29302
07-28 21:26:34.740 E/AndroidRuntime(29302): java.lang.IllegalStateException: Attempt to collect twice from pageEventFlow, which is an illegal operation. Did you forget to call Flow<PagingData<*>>.cachedIn(coroutineScope)?
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at kc.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at a6.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1385)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at a6.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:170)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2228)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:376)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:60)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:18)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at v21.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:302)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at k60.E(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at q01.X(dex-id-29c29fd48763180cb3ad3f2813af937996c3249d:142)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at qs0.b0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:9)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at eo0.e0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:62)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at eo0.start(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:5)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2309)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:391)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at x62.a(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:81)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at y02.k(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:116)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at y02.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at bt1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:73)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:75)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1156)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:332)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at em1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:147)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:216)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at fl0.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:214)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at c8.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:312)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at c8.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:77)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at o12.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:676)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at o12.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:93)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at em1.v(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:58)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:196)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:63)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at r90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:198)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at r90.f(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:67)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at jk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:94)
07-28 21:26:34.740 E/AndroidRuntime(29302): 	at jk.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:29)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at s.k0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:39)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at cx.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:44)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at cx.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at lk.o(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:280)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at kk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at n8.A(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at m8.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at android.os.Handler.handleCallback(Handler.java:942)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at android.os.Handler.dispatchMessage(Handler.java:99)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at android.os.Looper.loopOnce(Looper.java:226)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at android.os.Looper.loop(Looper.java:313)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at android.app.ActivityThread.main(ActivityThread.java:8762)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at java.lang.reflect.Method.invoke(Native Method)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:604)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1067)
07-28 21:26:34.741 E/AndroidRuntime(29302): 	Suppressed: d00: [qs0{Cancelling}@787618, Dispatchers.Main.immediate]
07-28 21:26:37.794 E/BufferQueueProducer( 2431): Unable to open libpenguin.so: dlopen failed: library "libpenguin.so" not found.
07-28 21:26:39.688 E/AndroidRuntime( 2431): FATAL EXCEPTION: main
07-28 21:26:39.688 E/AndroidRuntime( 2431): Process: io.github.leonhardweiler.gitnote.nightly, PID: 2431
07-28 21:26:39.688 E/AndroidRuntime( 2431): java.lang.IllegalStateException: Attempt to collect twice from pageEventFlow, which is an illegal operation. Did you forget to call Flow<PagingData<*>>.cachedIn(coroutineScope)?
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at kc.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at a6.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1385)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at a6.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:170)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2228)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:376)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:60)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:18)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at v21.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:302)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at k60.E(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at q01.X(dex-id-29c29fd48763180cb3ad3f2813af937996c3249d:142)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at qs0.b0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:9)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at eo0.e0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:62)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at eo0.start(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:5)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2309)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:391)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at x62.a(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:81)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at y02.k(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:116)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at y02.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at bt1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:73)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:75)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1156)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:332)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at em1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:147)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:216)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at fl0.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:214)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at c8.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:312)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at c8.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:77)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at o12.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:676)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at o12.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:93)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at em1.v(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:58)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:196)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:63)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at r90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:198)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at r90.f(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:67)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at jk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:94)
07-28 21:26:39.688 E/AndroidRuntime( 2431): 	at jk.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:29)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at s.k0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:39)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at cx.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:44)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at cx.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at lk.o(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:280)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at kk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at n8.A(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at m8.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at android.os.Handler.handleCallback(Handler.java:942)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at android.os.Handler.dispatchMessage(Handler.java:99)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at android.os.Looper.loopOnce(Looper.java:226)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at android.os.Looper.loop(Looper.java:313)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at android.app.ActivityThread.main(ActivityThread.java:8762)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at java.lang.reflect.Method.invoke(Native Method)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:604)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1067)
07-28 21:26:39.689 E/AndroidRuntime( 2431): 	Suppressed: d00: [qs0{Cancelling}@82466bf, Dispatchers.Main.immediate]
07-28 21:26:41.301 E/BufferQueueProducer( 2616): Unable to open libpenguin.so: dlopen failed: library "libpenguin.so" not found.
07-28 21:27:02.214 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:27:06.796 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:27:13.518 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:27:18.831 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:27:23.694 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:27:25.507 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:27:27.503 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:27:32.577 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:28:23.474 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:28:25.540 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:28:30.942 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:28:34.128 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:28:36.875 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:28:42.997 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:28:46.784 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:29:25.519 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:29:27.275 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:29:28.630 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:29:30.052 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:29:33.116 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:29:41.169 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:29:42.687 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:07.829 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:11.459 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:13.413 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:22.266 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:35.968 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:40.451 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:41.020 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:49.361 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:51.079 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:57.280 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:30:58.078 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:01.331 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:02.816 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:04.015 E/SQLiteLog( 2616): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:12.595 E/AndroidRuntime( 2616): FATAL EXCEPTION: main
07-28 21:31:12.595 E/AndroidRuntime( 2616): Process: io.github.leonhardweiler.gitnote.nightly, PID: 2616
07-28 21:31:12.595 E/AndroidRuntime( 2616): java.lang.IllegalStateException: Attempt to collect twice from pageEventFlow, which is an illegal operation. Did you forget to call Flow<PagingData<*>>.cachedIn(coroutineScope)?
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at kc.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at a6.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1385)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at a6.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:170)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2228)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:376)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:60)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:18)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at v21.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:302)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at k60.E(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at q01.X(dex-id-29c29fd48763180cb3ad3f2813af937996c3249d:142)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at qs0.b0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:9)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at eo0.e0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:62)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at eo0.start(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:5)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2309)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:391)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at x62.a(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:81)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at y02.k(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:116)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at y02.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at bt1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:73)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:75)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1156)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:332)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at em1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:147)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:216)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at fl0.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:214)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at c8.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:312)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at c8.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:77)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at o12.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:676)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at o12.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:93)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at em1.v(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:58)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:196)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:63)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at r90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:198)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at r90.f(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:67)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at jk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:94)
07-28 21:31:12.595 E/AndroidRuntime( 2616): 	at jk.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:29)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at s.k0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:39)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at cx.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:44)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at cx.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at lk.o(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:280)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at kk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at n8.A(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at m8.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at android.os.Handler.handleCallback(Handler.java:942)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at android.os.Handler.dispatchMessage(Handler.java:99)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at android.os.Looper.loopOnce(Looper.java:226)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at android.os.Looper.loop(Looper.java:313)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at android.app.ActivityThread.main(ActivityThread.java:8762)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at java.lang.reflect.Method.invoke(Native Method)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:604)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1067)
07-28 21:31:12.596 E/AndroidRuntime( 2616): 	Suppressed: d00: [qs0{Cancelling}@47f93bc, Dispatchers.Main.immediate]
07-28 21:31:13.726 E/BufferQueueProducer( 3012): Unable to open libpenguin.so: dlopen failed: library "libpenguin.so" not found.
07-28 21:31:16.204 E/AndroidRuntime( 3012): FATAL EXCEPTION: main
07-28 21:31:16.204 E/AndroidRuntime( 3012): Process: io.github.leonhardweiler.gitnote.nightly, PID: 3012
07-28 21:31:16.204 E/AndroidRuntime( 3012): java.lang.IllegalStateException: Attempt to collect twice from pageEventFlow, which is an illegal operation. Did you forget to call Flow<PagingData<*>>.cachedIn(coroutineScope)?
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at kc.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at a6.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1385)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at a6.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:170)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2228)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:376)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:60)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:18)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at v21.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:302)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at k60.E(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at q01.X(dex-id-29c29fd48763180cb3ad3f2813af937996c3249d:142)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at qs0.b0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:9)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at eo0.e0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:62)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at eo0.start(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:5)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2309)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:391)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at x62.a(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:81)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at y02.k(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:116)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at y02.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at bt1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:73)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:75)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1156)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:332)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at em1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:147)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:216)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at fl0.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:214)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at c8.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:312)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at c8.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:77)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at o12.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:676)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at o12.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:93)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at em1.v(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:58)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:196)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:63)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at r90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:198)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at r90.f(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:67)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at jk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:94)
07-28 21:31:16.204 E/AndroidRuntime( 3012): 	at jk.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:29)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at s.k0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:39)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at cx.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:44)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at cx.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at lk.o(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:280)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at kk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at n8.A(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at m8.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at android.os.Handler.handleCallback(Handler.java:942)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at android.os.Handler.dispatchMessage(Handler.java:99)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at android.os.Looper.loopOnce(Looper.java:226)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at android.os.Looper.loop(Looper.java:313)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at android.app.ActivityThread.main(ActivityThread.java:8762)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at java.lang.reflect.Method.invoke(Native Method)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:604)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1067)
07-28 21:31:16.206 E/AndroidRuntime( 3012): 	Suppressed: d00: [qs0{Cancelling}@dcb0689, Dispatchers.Main.immediate]
07-28 21:31:19.582 E/BufferQueueProducer( 3106): Unable to open libpenguin.so: dlopen failed: library "libpenguin.so" not found.
07-28 21:31:34.455 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:37.784 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:38.655 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:46.887 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:48.922 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:50.446 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:55.456 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:55.979 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:31:57.773 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:02.323 E/rust    ( 3106): git_wrapper: pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:32:02.325 E/GitManager( 3106): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:32:02.325 E/GitManager( 3106): java.lang.Exception: Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:32:02.325 E/GitManager( 3106): 	at af0.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:113)
07-28 21:32:02.325 E/GitManager( 3106): 	at af0.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:43)
07-28 21:32:02.325 E/GitManager( 3106): 	at io.github.leonhardweiler.gitnote.manager.GitManager.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:148)
07-28 21:32:02.325 E/GitManager( 3106): 	at io.github.leonhardweiler.gitnote.manager.GitManager.j(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:59)
07-28 21:32:02.325 E/GitManager( 3106): 	at v52.n(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:329)
07-28 21:32:02.325 E/GitManager( 3106): 	at v52.m(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:257)
07-28 21:32:02.325 E/GitManager( 3106): 	at v52.l(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:84)
07-28 21:32:02.325 E/GitManager( 3106): 	at w90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:168)
07-28 21:32:02.325 E/GitManager( 3106): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:32:02.325 E/GitManager( 3106): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:32:02.325 E/GitManager( 3106): 	at bu0.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:23)
07-28 21:32:02.325 E/GitManager( 3106): 	at m92.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:32:02.325 E/GitManager( 3106): 	at ku.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:88)
07-28 21:32:02.325 E/StorageManager( 3106): Can't pull: fetch: failed to resolve address for github.com: No address associated with hostname; class=Net (12)
07-28 21:32:14.845 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:19.955 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:22.126 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:25.765 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:28.322 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:29.064 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:47.330 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:52.810 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:53.392 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:32:59.046 E/SQLiteLog( 3106): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:33:58.183 E/AndroidRuntime( 3106): FATAL EXCEPTION: main
07-28 21:33:58.183 E/AndroidRuntime( 3106): Process: io.github.leonhardweiler.gitnote.nightly, PID: 3106
07-28 21:33:58.183 E/AndroidRuntime( 3106): java.lang.IllegalStateException: Attempt to collect twice from pageEventFlow, which is an illegal operation. Did you forget to call Flow<PagingData<*>>.cachedIn(coroutineScope)?
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at kc.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at a6.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1385)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at a6.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:170)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2228)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:376)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:60)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:18)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at v21.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:302)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at k60.E(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at q01.X(dex-id-29c29fd48763180cb3ad3f2813af937996c3249d:142)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at qs0.b0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:9)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at eo0.e0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:62)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at eo0.start(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:5)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2309)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:391)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at x62.a(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:81)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at y02.k(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:116)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at y02.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at bt1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:73)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:75)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1156)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:332)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at em1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:147)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:216)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at fl0.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:214)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at c8.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:312)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at c8.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:77)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at o12.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:676)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at o12.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:93)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at em1.v(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:58)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:196)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:63)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at r90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:198)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at r90.f(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:67)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at jk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:94)
07-28 21:33:58.183 E/AndroidRuntime( 3106): 	at jk.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:29)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at s.k0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:39)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at cx.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:44)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at cx.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at lk.o(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:280)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at kk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at n8.A(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at m8.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at android.os.Handler.handleCallback(Handler.java:942)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at android.os.Handler.dispatchMessage(Handler.java:99)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at android.os.Looper.loopOnce(Looper.java:226)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at android.os.Looper.loop(Looper.java:313)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at android.app.ActivityThread.main(ActivityThread.java:8762)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at java.lang.reflect.Method.invoke(Native Method)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:604)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1067)
07-28 21:33:58.184 E/AndroidRuntime( 3106): 	Suppressed: d00: [qs0{Cancelling}@98e3692, Dispatchers.Main.immediate]
07-28 21:33:59.690 E/BufferQueueProducer( 3425): Unable to open libpenguin.so: dlopen failed: library "libpenguin.so" not found.
07-28 21:34:33.804 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:34:36.816 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:34:47.578 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:34:51.402 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:34:57.056 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:34:58.076 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:35:00.865 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:35:08.455 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:35:09.982 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:35:21.068 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:35:23.195 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:35:27.589 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:35:30.578 E/SQLiteLog( 3425): (1555) abort at 30 in [INSERT INTO `Notes` (`relativePath`,`content`,`lastModifiedTimeMillis`,`id`,`parentPath`,`fileName`) VALUES (?,?,?,?,?,?)]: UNIQUE constraint failed: Notes.relativePath
07-28 21:35:35.870 E/AndroidRuntime( 3425): FATAL EXCEPTION: main
07-28 21:35:35.870 E/AndroidRuntime( 3425): Process: io.github.leonhardweiler.gitnote.nightly, PID: 3425
07-28 21:35:35.870 E/AndroidRuntime( 3425): java.lang.IllegalStateException: Attempt to collect twice from pageEventFlow, which is an illegal operation. Did you forget to call Flow<PagingData<*>>.cachedIn(coroutineScope)?
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at kc.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at a6.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1385)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at a6.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:170)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2228)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:376)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:60)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:18)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at v21.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:302)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at k60.E(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at q01.X(dex-id-29c29fd48763180cb3ad3f2813af937996c3249d:142)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at qs0.b0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:9)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at eo0.e0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:62)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at eo0.start(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:5)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at g.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:2309)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at g.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:391)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at x62.a(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:81)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at y02.k(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:116)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at y02.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at bt1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:73)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:75)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:1156)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:332)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at em1.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:147)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at l90.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:216)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at fl0.d(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:214)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at c8.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:312)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at c8.i(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:77)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at o12.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:676)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at o12.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:93)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at c02.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:19)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at kb1.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:11)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at em1.v(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:58)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at e.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:196)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at e.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:63)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at r90.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:198)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at r90.f(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:67)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at jk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:94)
07-28 21:35:35.870 E/AndroidRuntime( 3425): 	at jk.h(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:29)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at s.k0(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:39)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at cx.r(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:44)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at cx.s(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at lk.o(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:280)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at kk.u(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:13)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at ge.g(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:8)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at r00.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:114)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at n8.A(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:24)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at m8.run(r8-map-id-a4a4bd3568d05c697f210bb5b41e781bdef4ac8e200147b2b452f75eadeb7490:3)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at android.os.Handler.handleCallback(Handler.java:942)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at android.os.Handler.dispatchMessage(Handler.java:99)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at android.os.Looper.loopOnce(Looper.java:226)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at android.os.Looper.loop(Looper.java:313)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at android.app.ActivityThread.main(ActivityThread.java:8762)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at java.lang.reflect.Method.invoke(Native Method)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:604)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1067)
07-28 21:35:35.871 E/AndroidRuntime( 3425): 	Suppressed: d00: [qs0{Cancelling}@a4e5d1b, Dispatchers.Main.immediate]
07-28 21:35:39.182 E/BufferQueueProducer( 3720): Unable to open libpenguin.so: dlopen failed: library "libpenguin.so" not found.
