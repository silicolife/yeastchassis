load.frequencies <- function(frequenciesFile){
 data <- read.csv(frequenciesFile, h=T)
 data <- data[,-c(2)]
 rownames(data) <- data[,1]
 data[,1] <- NULL
 data
}

load.ssfiles <- function(files,maxyield,targetcarbons,substrateCarbons,outputFile=NULL){
  frequencies = matrix(0, length(files),5)
  
  for(i in 1:length(files)){
  	print(paste("Reading: ",files[i]))
    tmp = read.csv(pipe(paste("cut -d, -f1",files[i])),h=F)
    until = 0
    if(length(tmp$V1) < 100)
      until = length(tmp$V1)
    else
      until = 100

    tmp <- sort(tmp$V1,decreasing=T)[1:until]
    corrected <- c(100)
    for(j in 1: length(tmp)){
      if(tmp[j]> 1e-5)
        corrected[j] = tmp[j] + (tmp[j]*0.1)
      else
        corrected[j] = 0.0
    }
    
    yields <- corrected * targetcarbons
    yields <- yields / substrateCarbons
    
    frequencies[i,1] = length(yields[yields>(maxyield*0.01)])/until
    frequencies[i,2] = length(yields[yields>(maxyield*0.05)])/until
    frequencies[i,3] = length(yields[yields>(maxyield*0.1)])/until
    frequencies[i,4] = length(yields[yields>(maxyield*0.25)])/until
    frequencies[i,5] = length(yields[yields>(maxyield*0.50)])/until
  }
  
  frequencies <- as.data.frame(frequencies)
  colnames(frequencies) <- c("1%","5%","10%","25%","50%")
  
  print(frequencies)
  
  pdf(file=outputFile,width=10,height=10)
  boxplot(frequencies,xlab="percentage of maximum theoretical C-yield",ylab="percentage of top 100 solutions",ylim=c(0,1))
  dev.off()

}

load.convergence.data.invista <- function(files,maxevals,maxyield,targetCarbons,substrateCarbons){
    
  fbadata = matrix(0, maxevals,length(files))
  for(i in 1:length(files)){
    print(paste("Reading: ",files[i]))
    tmp = read.csv(pipe(paste("cut -d, -f1,2",files[i])))
    if(dim(tmp)[1] > 0){
	    for(j in 1:(dim(tmp)[1])){
	      if(tmp[j,1]<=maxevals){
	        fbadata[tmp[j,1],i]=tmp[j,2]
	      }
	    }
    }
  }
  
  #print(fbadata)
  
  dimdata = dim(fbadata)
  nrows = dimdata[1]
  ncols = dimdata[2]
  
  for(i in 1:ncols){
    best =0
    yield = 0
    for(j in 1:nrows){
      yield <- fbadata[j,i] * targetCarbons
      yield <- yield / substrateCarbons
      if( yield > best ){
        best=yield
      }
      
      fbadata[j,i] = best
    }
  }
  fbadata
}

load.convergence.data.invista.bioyield <- function(files,maxevals,factor){
    
  fbadata = matrix(0, maxevals,length(files))
  for(i in 1:length(files)){
    print(paste("Reading: ",files[i]))
    tmp = read.csv(pipe(paste("cut -d, -f1,2",files[i])))
    if(dim(tmp)[1] > 0){
	    for(j in 1:(dim(tmp)[1])){
	      if(tmp[j,1]<=maxevals){
	        fbadata[tmp[j,1],i]=tmp[j,2]
	      }
	    }
    }
  }
  
  #print(fbadata)
  
  dimdata = dim(fbadata)
  nrows = dimdata[1]
  ncols = dimdata[2]
  
  for(i in 1:ncols){
    best =0
    yield = 0
    for(j in 1:nrows){
      yield <- fbadata[j,i] * factor
      if( yield > best ){
        best=yield
      }
      
      fbadata[j,i] = best
    }
  }
  fbadata
}

load.convergence.data <- function(files,maxevals){
    
  fbadata = matrix(0, maxevals,length(files))
  for(i in 1:length(files)){
    print(paste("Reading: ",files[i]))
    tmp = read.csv(pipe(paste("cut -d, -f1,2",files[i])))
    if(dim(tmp)[1] > 0){
	    for(j in 1:(dim(tmp)[1])){
	      if(tmp[j,1]<=maxevals){
	        fbadata[tmp[j,1],i]=tmp[j,2]
	      }
	    }
    }
  }
  
  #print(fbadata)
  
  dimdata = dim(fbadata)
  nrows = dimdata[1]
  ncols = dimdata[2]
  
  for(i in 1:ncols){
    best =0
    for(j in 1:nrows){
      if( fbadata[j,i] > best ){
        best=fbadata[j,i]
      }
      
      fbadata[j,i] = best
    }
  }
  fbadata
}

averages <- function(data,max=FALSE){
  
  n = dim(data)[1]
  if(max==TRUE)
    avg = matrix(,n,4)
  else 
    avg = matrix(,n,3)
  
  for(i in 1:n){
    avg[i,1] = mean(data[i,])
    stdev = sd(data[i,])
    #	error = qnorm(0.975)*stdev/sqrt(n) 
    error = stdev
    if((avg[i,1]-error)>0)
      avg[i,2] = avg[i,1]-error
    else 
      avg[i,2] = 0
    
    avg[i,3] = avg[i,1]+error
    
    if(max==TRUE)
      avg[i,4] = max(data[i,])
  }
  
  avg
}

averages.fr <- function(data,includeMax=FALSE){
  print("Computing averages...")
  avg = averages(data,includeMax);
  evals = seq(1:dim(data)[1])
  means = avg[,1]
  lows  = avg[,2]
  highs = avg[,3]
  
  if(includeMax==TRUE){
    maxs = avg[,4]
    df = data.frame(eval=evals, mean=means, low=lows, high=highs, max = maxs)
  }
  else{
    df = data.frame(eval=evals, mean=means, low=lows, high=highs)
  }
  
  df
}

bind.data <- function(listdata){
  
  df<- listdata[[1]]
  for(i in 2:length(listdata)){
    df_aux <- listdata[[i]]
    df <- rbind(df,df_aux)
  }
    
  df
}

#LOAD FITNESS AND DISTANCES DATA
load.fitdist.data <- function(filedist,filefit){
  
  data_dist = read.csv(filedist,h=T)
  data_fit = read.csv(filefit,h=T)
  
  ncols = dim(data_dist)[2]
  columnNames = colnames(data_dist)
  
  finalDF = NULL
  for(i in 2:(ncols-1)){
    df <- data.frame(
          id = data_dist$ID,
          dist = data_dist[,i],
          fit = data_fit[,i],
          fluxdist = columnNames[i],
          generator = data_dist$GENERATOR
      )
    if(is.null(finalDF)){
      finalDF <- df
    }else{
      finalDF <- rbind(finalDF,df)
    }     
  }
  finalDF
}

load.fitness.data <- function(filefit){
	data <- read.csv(filefit,h=T)
	df = data.frame(id=data$ID, pfba=data$PFBA, lmoma=data$LMOMA, generator=data$GENERATOR)
}

load.results.data <- function(results){
    data <- read.csv(results, h=T)
    ncols = dim(data)[2]
    columnNames = colnames(data)
    
    finalDF = NULL
    for(i in 2:(ncols-1)){
   		df <- data.frame(
      		id = data$ID,
      	 	value = data[,i],
      	 	of = columnNames[i]
    	)
    	if(is.null(finalDF)){
       		finalDF <- df
	    }else{
	    	finalDF <- rbind(finalDF,df)
	    }
	}
  finalDF
}

load.results.data <- function(results,indexes=NULL){
    data <- read.csv(results, h=T)
    #print(data[1:5,])
    ncols = dim(data)[2]
    columnNames = colnames(data)
    
    if(is.null(indexes)==FALSE){
    	indexes = indexes +2
    }else{
    	indexes = c(2:(ncols-1))
    }
    
 #   print(indexes)
    
    finalDF = NULL
    for(i in indexes){
   		df <- data.frame(
      		id = data$ID,
      	 	value = data[,i],
      	 	of = columnNames[i]
    	)
    	if(is.null(finalDF)){
       		finalDF <- df
	    }else{
	    	finalDF <- rbind(finalDF,df)
	    }
	}
  finalDF
}

load.distfitcorr.data <- function(filedist,filefit){
  
  data_dist = read.csv(filedist,h=T)
  data_fit = read.csv(filefit,h=T)
  
  df <- data.frame(
    id=data_dist$ID,
    dist_pfba=data_dist$PFBA, 
    dist_lmoma=data_dist$LMOMA,
    fit_pfba=data_fit$PFBA,
    fit_lmoma=data_fit$LMOMA,
    generator=data_fit$GENERATOR
    )
}

load.results.simple <- function(fileRes, colinclude=NULL){
  data <- read.csv(fileRes,h=T)
  if(is.null(colinclude)==FALSE){
    colinclude <- colinclude+2
    colinclude <-c(1,colinclude)
    print(colinclude)
    data <- data[,colinclude]
  }
  data
}

load.results.standard2systematic <- function(fileResults, fileGIDS, colinclude=NULL){
  data <- read.csv(fileResults,h=T)
  gids <- read.csv(fileGIDS,h=F,sep="\t")
  if(is.null(colinclude)==FALSE){
    colinclude <- colinclude+2
    colinclude <-c(1,colinclude)
    #print(colinclude)
    data <- data[,colinclude]
  }
  ncols = dim(data)[2]
  nrows = dim(data)[1]
  newcolumn = c(nrows)
  sol = data[,ncols]
  for(i in 1:nrows){
    #print(paste(i,"/",nrows))
    if(as.character(sol[i])!=""){
      tokens = unlist(strsplit(as.character(sol[i]),"\\s"))
      newSolution <- NULL
      for(j in 1:length(tokens)){
        id = tokens[j]
        newID = as.character(gids[gids$V1==id,2])
        if(length(newID) ==0 || newID==""){
          newID = id
        }
        if(is.null(newSolution)){
          newSolution = newID
        }else{
          newSolution = paste(newSolution,newID, sep=" ")
        }
      }
      newcolumn[i] <- newSolution
    }
  }
  
  data[,ncols] <- as.data.frame(newcolumn)
  data
}


filter.df.by.top <- function(df,of,percent){
  max <- max(df$value[df$of==of])
  minval <- max*percent
  ids <- df$id[df$of==of & df$value >= minval]
  data[df$id %in% ids,]  
}

filter.table.by.top <- function(table, of, percent){
  max <- max(table[,of])
  minval <- max*percent
  ids <- table$ID[table[,of] >= minval]
  table[table$ID %in% ids,]
}


process.frequency.data <- function(data,maxknocks,maxresults=NULL){
  print(head(data))
  print(maxresults)
  solutions <- data
  out <- str_split_fixed(solutions$SOLUTION," ",maxknocks)
  df <- data.frame(id=solutions$ID,out)
  df <- sapply(df,blank2na)
  freq = sort(table(unlist(df[,-1])))/dim(df)[1]
  count = sort(table(unlist(df[,-1])))
  ord = order(freq,decreasing=F)
  ids = unlist(labels(freq))
  fac = factor(ids,levels=ids[ord])
  freqdf <- data.frame(id=fac,freqs=freq,counts=count)
  
  
  nrows = dim(freqdf)[1]
  min = 1
  if(is.null(maxresults)==FALSE){
  	if(nrows>maxresults)
  		min <- nrows - maxresults
  }
  freqdf <- freqdf[min:nrows,]
  
  freqdf
}

process.groupfrequency.data <- function(filteredTable,maxknocks,kmax,maxresults=NULL){
  out <- str_split_fixed(filteredTable$SOLUTION," ",maxknocks)
  nsols <- dim(out)[1]
  listcombs <- list()
  for(i in 1:nsols){
    sol <- out[i,]
    sol <- sol[!is.na(blank2na(sol))]
    if(length(sol) >= kmax){
	    combmat <- combn(sol,kmax,simplify=FALSE)
	    if(length(listcombs)==0)
	      listcombs <- combmat
	    else
	      listcombs <- append(listcombs,combmat)
	}
  }
  
  sortedlist <- list()
  for(i in 1:length(listcombs)){
    l <- list(sort(unlist(listcombs[i])))
    if(length(sortedlist)==0)
      sortedlist <- l
    else
      sortedlist <- append(sortedlist,l)
  }
  uni <- unique(sortedlist)
  
  ids = array(,length(uni))
  count = array(,length(uni))
  freq= array(,length(uni))
  for(i in 1:length(uni)){
    comb <- uni[i]
    combcount = 0;
    for(j in 1: nsols){
      sol <- out[j,]
      inter <- intersect(unlist(comb),sol)
      if(length(inter)==kmax)
        combcount = combcount+1
    }
    ids[i] <- paste(unlist(comb),collapse=" | ")
    count[i] <- combcount
    freq[i] <- combcount/nsols
  }
  
  ord <- order(freq,decreasing=F)
  fac <- factor(ids,levels=ids[ord])
  
  freqdf <- data.frame(id=fac,counts=count,freqs=freq)
  
  nrows = dim(freqdf)[1]
  min = 1
  if(is.null(maxresults)==FALSE){
  	if(nrows>maxresults)
  		min <- nrows - maxresults
  }
  
  freqdf <- freqdf[order(freqdf$counts),]
  freqdf <- freqdf[min:nrows,]
 
  freqdf
}

load.perRunMetrics.data <- function(results,indexes=NULL){
  data <- read.csv(results, h=T)
  ncols = dim(data)[2]
  columnNames = colnames(data)
  
  if(is.null(indexes)==FALSE){
    indexes = indexes +2
  }else{
    indexes = c(2:(ncols))
  }
  
  #   print(indexes)
  
  finalDF = NULL
  for(i in indexes){
    df <- data.frame(
      id = data$RUN,
      value = data[,i],
      of = columnNames[i]
    )
    if(is.null(finalDF)){
      finalDF <- df
    }else{
      finalDF <- rbind(finalDF,df)
    }
  }
  finalDF
}

load.pareto.data <- function(results,index1,index2){
  data <- read.csv(results, h=T)
  ncols = dim(data)[2]
  columnNames = colnames(data)
  
  index1 = index1 +2
  index2 = index2 +2
  
  finalDF = NULL
  df <- data.frame(
    id = data$ID,
    value1 = data[,index1],
    value2 = data[,index2]
  )
  if(is.null(finalDF)){
    finalDF <- df
  }else{
    finalDF <- rbind(finalDF,df)
  }
  
  max1 = max(df$value1)
  max2 = max(df$value2)  
  ids = c("lim1","lim2")
  p1s = c(max1,0)
  p2s = c(0,max2)
  dfextra <- data.frame(
    id = ids,
    value1=p1s,
    value2=p2s
    )
  finalDF <- rbind(finalDF,dfextra)
  finalDF
}