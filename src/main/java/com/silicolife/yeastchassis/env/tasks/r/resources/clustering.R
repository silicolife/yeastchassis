plot.clusters <- function(data, k, method="pam",title=NULL, xlab=NULL, ylab=NULL,facet=FALSE){
  
  dm <- cbind(as.matrix(data$pfba),as.matrix(data$lmoma))
  
  if(method=="pam"){
    print(paste("computing pam (K=",k,")... wait",sep=""))
    set.seed(CLUSTERING_SEED)
    fit <- pam(dm,k)
  } else if(method=="kmeans"){
    print(paste("computing kmeans (K=",k,")... wait",sep=""))
    set.seed(CLUSTERING_SEED)
    fit <- kmeans(dm,k,iter.max=1000,nstart=100)
  }
  
  clusdata <- data.frame(data,cluster=factor(fit$cluster))
  clusdata$label=paste("K=",k,sep="")
  
  clusters <- clusdata
  clusters <- na.omit(clusters)
  clusters <- droplevels(clusters)
  clu <- ddply(clusters,"cluster",find_hull)
  
  clusplot <- ggplot(data=clusdata, aes(x=pfba, y=lmoma, colour = cluster, fill=cluster)) + 
    geom_point(size=3,alpha=1/3,aes(shape=generator)) +    
    scale_colour_manual(values=cbPalette) +
    scale_fill_manual(values=cbPalette) + 
    scale_shape_manual(values=c(1,4,5,6,15,16,17,18,25)) +    
    geom_polygon(data=clu,aes(group=cluster),alpha=0.3,size=.2) +
    guides(shape = guide_legend(order = 1)) + 
    theme_bw() + 
  
  if(is.null(title)==FALSE){clusplot <- clusplot + ggtitle(title)}
  if(is.null(xlab)==FALSE){clusplot <- clusplot + xlab(xlab)}
  if(is.null(ylab)==FALSE){clusplot <- clusplot + ylab(ylab)}
  if(facet==TRUE){clusplot <- clusplot + facet_grid(. ~ label)}
  
  clusplot
}

ggplot.multiclust <- function(data,method="pam",outputFile=NULL){

  xlabel <- expression(BPCY-pFBA (italic(mmol %.% gDW^-1 %.% h^-1)))
  ylabel <- expression(BPCY-LMOMA (italic(mmol %.% gDW^-1 %.% h^-1)))
  
  plot_k3 <- plot.clusters(data,3,method,facet=T,xlab=xlabel,ylab=ylabel)
  plot_k4 <- plot.clusters(data,4,method,facet=T,xlab=xlabel,ylab=ylabel)
  plot_k5 <- plot.clusters(data,5,method,facet=T,xlab=xlabel,ylab=ylabel)
  plot_k6 <- plot.clusters(data,6,method,facet=T,xlab=xlabel,ylab=ylabel)
  plot_k7 <- plot.clusters(data,7,method,facet=T,xlab=xlabel,ylab=ylabel)
  plot_k8 <- plot.clusters(data,8,method,facet=T,xlab=xlabel,ylab=ylabel)
  
  pdf(file=outputFile,width=10,height=10)
  grid.arrange(plot_k3, plot_k4, plot_k5, plot_k6, plot_k7, plot_k8, ncol=2, nrow=3)
  dev.off()
}

ggplot.distclusters <- function(data, k, method="pam",facet=FALSE, title=NULL, xlab=NULL, ylab=NULL){
  
  dm <- cbind(as.matrix(data$fit_pfba),as.matrix(data$fit_lmoma))
  
  if(method=="pam"){
    print(paste("computing pam (K=",k,")... wait",sep=""))
    set.seed(CLUSTERING_SEED)
    fit <- pam(dm,k)
  } else if(method=="kmeans"){
    print(paste("computing kmeans (K=",k,")... wait",sep=""))
    set.seed(CLUSTERING_SEED)
    fit <- kmeans(dm,k,iter.max=1000,nstart=100)
  }
  
  clusdata <- data.frame(data,cluster=factor(fit$cluster))
  clusdata$label=paste("K=",k,sep="")
  clusdata$Agg_fitness = data$fit_pfba + data$fit_lmoma
  
  maxpointFitness = (max(data$fit_pfba) +  max(data$fit_lmoma))
  minpointFitness = (min(data$fit_pfba) +  min(data$fit_lmoma))
  midpointFitness = (maxpointFitness+minpointFitness)/2
  
  print(midpointFitness)
  
  
  clusters <- clusdata
  clusters <- na.omit(clusters)
  clusters <- droplevels(clusters)
  clu <- ddply(clusters,"cluster",find_hull_dist)
  
  clusplot <- ggplot(data=clusdata, aes(x=dist_pfba, y=dist_lmoma, colour = Agg_fitness, fill=Agg_fitness,shape=cluster)) + 
    geom_point(size=2) +    
    scale_color_gradient2(low = "black",mid="orange", high = "yellow",  midpoint=midpointFitness, guide = "colourbar") +
    scale_fill_gradient2(low = "black",mid="orange", high = "yellow",  midpoint=midpointFitness, guide = "colourbar") +
    scale_shape_manual(values=c(15,16,17,18,25,0,1,3)) +
    theme_bw() + 
    geom_polygon(data=clu,alpha=.5,size=.5,lineend="round") +
    xlim(0,1.0) + 
    ylim(0,1.0)
    
  
  if(is.null(title)==FALSE){clusplot <- clusplot + ggtitle(title)}
  if(is.null(xlab)==FALSE){clusplot <- clusplot + xlab(xlab)}
  if(is.null(ylab)==FALSE){clusplot <- clusplot + ylab(ylab)}
  if(facet==TRUE){clusplot <- clusplot + facet_grid(. ~ label)}
  
  clusplot
}

ggplot.hclust.variables <- function(bounddata,title=NULL,xlab=NULL,ylab="Jaccard distance"){
	cols = unique(bounddata$id)
	prods <- levels(bounddata$var)	
	
	finaldf <- matrix(NA,length(prods),length(cols))
	colnames(finaldf) <- cols
	rownames(finaldf) <- prods
	
	for(i in 1:length(prods)){
	  prod <- prods[i]
	  r_in_prod <- bounddata$id[bounddata$var==prod]
	# print(prod)
	# print(r_in_prod)
 	  row = c()
	  for(j in 1:length(cols)){
	    if((cols[j] %in% r_in_prod)==TRUE){
	      finaldf[i,j] <- 1
	    }else{
	      finaldf[i,j] <- 0
	    }
	  }
	}
	
	dm <- dist(finaldf,method="binary")
	print(dm)
	hc <- hclust(dm)
	dend <- ggdendrogram(hc,theme_dendro=FALSE) +
	theme(legend.position="none",axis.title.x=element_blank())
	if(is.null(title)==FALSE){dend <- dend + ggtitle(title)}
  	if(is.null(xlab)==FALSE){dend <- dend + xlab(xlab)}
  	if(is.null(ylab)==FALSE){dend <- dend + ylab(ylab)}
  	
  	dend
}

heatmap.variables <- function(bounddata,outputFile){

	cols = unique(bounddata$id)
	prods <- levels(bounddata$var)
	#my_palette <- colorRampPalette(c("gray", "green", "blue"))(n = 299)
	my_palette <- colorRampPalette(c("#FFFFCC","#E68A00","#CC0000"))(n = 299)	
	
	finaldf <- matrix(NA,length(prods),length(cols))
	colnames(finaldf) <- cols
	rownames(finaldf) <- prods
	for(i in 1:length(prods)){
	  prod <- prods[i]
	  r_in_prod <- bounddata$id[bounddata$var==prod]
	  data_prod <- bounddata[bounddata$var==prod,]
	  for(j in 1:length(cols)){
	    if((cols[j] %in% r_in_prod)==TRUE){
	      finaldf[i,j] <- data_prod$freq[data_prod$id==cols[j]]
	    }else{
	      finaldf[i,j] <- 0.0
	    }
	  }
	}
	finaldf <- finaldf[,order(colSums(finaldf),decreasing=T)]
	finaldf <- t(finaldf[,colSums(finaldf)>0.5])
	#finaldf <- t(finaldf)
	#finaldf <- t(finaldf[,1:30])	
	pdf(file=outputFile,width=6,height=10)
	marg = c(7,5)
	distances <- function(c){
  		d <- dist(c,method="euclidean")
	}
	#colLabels <- c("FA","SA","MA")
	#heatmap.2(finaldf,sepwidth=c(0.0,0.0),trace="none",distfun=distances,margins=marg,dendrogram="both",cexRow=1.0,cexCol=1.2,labCol=colLabels,keysize=1.2,srtCol=0,col=my_palette,density.info="none")
	#my.heatmap(finaldf,sepwidth=c(0.0,0.0),trace="none",distfun=distances,margins=marg,dendrogram="both",cexRow=1.0,cexCol=1.2,labCol=colLabels,keysize=1.2,srtCol=0,col=my_palette,density.info="none",keylabel="Frequency")
	#my.heatmap(finaldf,sepwidth=c(0.0,0.0),trace="none",distfun=distances,margins=marg,dendrogram="both",cexRow=1.0,cexCol=1.2,keysize=1.2,srtCol=45,col=my_palette,density.info="none",keylabel="Frequency")
	my.heatmap(finaldf,sepwidth=c(0.0,0.0),trace="none",distfun=distances,margins=marg,dendrogram="both",cexRow=0.8,cexCol=1.0,keysize=1.2,srtCol=45,col=my_palette,density.info="none",keylabel="Frequency")
	dev.off()
}

heatmap.variables.2 <- function(data,outputFile){

	my_palette <- colorRampPalette(c("#FFFFCC","#E68A00","#CC0000"))(n = 299)
	data <- data.matrix(data)
	data <- data[rowSums(data)>0.5,]
	print(outputFile)
	pdf(file=outputFile,width=6,height=10)
	marg = c(7,5)
	distances <- function(c){
  		d <- dist(c,method="euclidean")
	}	
	my.heatmap(data,sepwidth=c(0.0,0.0),trace="none",distfun=distances,margins=marg,dendrogram="both",cexRow=0.8,cexCol=1.0,keysize=1.2,srtCol=45,col=my_palette,density.info="none",keylabel="Frequency")
	dev.off()
}
