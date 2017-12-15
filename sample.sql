--relative-dir	sample-tsv
--bind-param	dname=SALES
--bind-param	bottom=1000
--bind-param	top=2000
--bind-param	begin=1981/02/01
--bind-param	end=1981/02/28

-- DEPT
select * from DEPT d
where DNAME like :dname;

-- DEPT, EMP
select * from DEPT d inner join EMP e on d.DEPTNO = e.DEPTNO
where d.DNAME like :dname
  and e.SAL between :bottom and :top
  and e.HIREDATE between :begin and :end;
