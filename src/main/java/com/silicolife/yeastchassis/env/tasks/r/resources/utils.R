find_hull <- function(df) df[chull(df$pfba, df$lmoma), ]
find_hull_dist <- function(df) df[chull(df$dist_pfba, df$dist_lmoma), ]
find_hull_fit <- function(df) df[chull(df$fit_pfba, df$fit_lmoma), ]
find_poly <- function(df) ncpoly(df)
find_poly_drop <- function(df) ncpoly_drop(df)
find_poly_ref <- function(df) ncpoly_ref(df)
find_area <- function(df) parea(df$value1,df$value2)

blank2na <- function(x){ 
  z <- gsub("\\s+", "", x)
  x[z==""] <- NA 
  return(x)
}

jaccard.index <- function(setA, setB){
  if(length(setA)==0 & length(setB)==0){
    jaccard = 1
  }
  else{
    jaccard = length(intersect(setA,setB))/ length(union(setA,setB))
  }
  jaccard
}

jaccard.distance <- function(setA, setB){
  jaccard <- 1- jaccard.index(setA,setB)
}

ncpoly <- function(df){
  require(plyr)
  df <- arrange(df,desc(value1),value2)
  ncols <- dim(df)[2]
  if(ncols==4){
  	df2 <- data.frame(id=c("origin"),value1=c(0),value2=c(0),var=df$var[1])
  		df <- rbind(df,df2)
  }
  else if(ncols==5){
	var2levels <- levels(df$var2)
	for(i in 1: length(var2levels)){
  		df2 <- data.frame(id=c("origin"),value1=c(0),value2=c(0),var=df$var[1],var2=var2levels[i])
  		df <- rbind(df,df2)
  	}
  }
  df
}

ncpoly_drop <- function(df){
  require(plyr)
  df <- arrange(df,desc(value1),value2)
  nrows = dim(df)[1]
  ncols <- dim(df)[2]
  df_new <- data.frame()
  if(ncols==4){
	  for(i in 1:nrows){
	    df_actual <- data.frame(id=df[i,1],value1=df[i,2],value2=df[i,3],var=df[i,4])
	    df_new<- rbind(df_new,df_actual)
	    if(i<nrows){
	      v1 = df[i+1,2]
	      v2 = df[i,3]
	      df_aux <- data.frame(id=paste("drop_",i,sep=""),value1=v1,value2=v2,var=df[i,4])
	      df_new <- rbind(df_new,df_aux)
	    }
	  }
	  df2 <- data.frame(id=c("origin"),value1=c(0),value2=c(0),var=df$var[1])
	  df_new <- rbind(df_new,df2)
  }
  else if(ncols==5){
  	for(i in 1:nrows){
	    df_actual <- data.frame(id=df[i,1],value1=df[i,2],value2=df[i,3],var=df[i,4],var2=df[i,5])
	    df_new<- rbind(df_new,df_actual)
	    if(i<nrows){
	      v1 = df[i+1,2]
	      v2 = df[i,3]
	      df_aux <- data.frame(id=paste("drop_",i,sep=""),value1=v1,value2=v2,var=df[i,4],var2=df[i,5])
	      df_new <- rbind(df_new,df_aux)
	    }
	  }
	  var2levels <- levels(df$var2)
	  for(i in 1: length(var2levels)){
  		df2 <- data.frame(id=c("origin"),value1=c(0),value2=c(0),var=df$var[1],var2=var2levels[i])
  		df_new <- rbind(df_new,df2)
	   }
  }
  df_new
  
}



ncpoly_ref <- function(df){
  require(plyr)
  df <- arrange(df,desc(value1),value2)
  df2 <- data.frame(id=c("origin"),value1=c(0),value2=c(0),var2=df$var2[1])
  df <- rbind(df,df2)
  
}

ncpoly2 <- function(df){
  require(plyr)
  df <- arrange(df,desc(value1),value2)
  df2 <- data.frame(id=c("origin"),value1=c(0),value2=c(0))
  df <- rbind(df,df2)
}

parea <- function(x,y){
  require(pracma)
  polyarea(x,y)
}