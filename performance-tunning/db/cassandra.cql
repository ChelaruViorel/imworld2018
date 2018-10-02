create table tmp_viorel_perftune(
	file_id uuid,
	article text,
	variant int, 
	bundle int, 
	store int,
	size int,
	price float,
	manufacture_date timestamp,
	valability_days int,
	primary key(file_id, article, variant, bundle)
);