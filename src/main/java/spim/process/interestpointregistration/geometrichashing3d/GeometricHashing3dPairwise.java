package spim.process.interestpointregistration.geometrichashing3d;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;

import mpicbg.models.AffineModel3D;
import mpicbg.models.Model;
import mpicbg.models.RigidModel3D;
import mpicbg.models.TranslationModel3D;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.process.interestpointregistration.ChannelInterestPointListPair;
import spim.process.interestpointregistration.Detection;
import spim.process.interestpointregistration.RANSAC;
import spim.process.interestpointregistration.RANSACParameters;

public class GeometricHashing3dPairwise implements Callable< ChannelInterestPointListPair >
{
	final ChannelInterestPointListPair pair;
	final int model;
	final RANSACParameters rp;
	final GeometricHashing3dParameters gp;
	final String comparison;
	
	public GeometricHashing3dPairwise( final ChannelInterestPointListPair pair, final int model, final String comparison, final RANSACParameters rp, final GeometricHashing3dParameters gp )
	{ 
		this.pair = pair;
		this.rp = rp;
		this.gp = gp;
		this.model = model;
		this.comparison = comparison;
	}

	public GeometricHashing3dPairwise( final ChannelInterestPointListPair pair, final int model, final String comparison, final RANSACParameters rp )
	{
		this( pair, model, comparison, rp, new GeometricHashing3dParameters() );
	}

	public GeometricHashing3dPairwise( final ChannelInterestPointListPair pair, final int model, final String comparison )
	{
		this( pair, model, comparison, new RANSACParameters(), new GeometricHashing3dParameters() );
	}
	
	@Override
	public ChannelInterestPointListPair call() throws Exception 
	{
		final GeometricHasher3d hasher = new GeometricHasher3d();
		
		final ArrayList< Detection > listA = new ArrayList< Detection >();
		final ArrayList< Detection > listB = new ArrayList< Detection >();
		
		for ( final InterestPoint i : pair.getListA() )
			listA.add( new Detection( i.getId(), i.getL() ) );

		for ( final InterestPoint i : pair.getListB() )
			listB.add( new Detection( i.getId(), i.getL() ) );

		final ArrayList< PointMatchGeneric< Detection > > candidates = hasher.extractCorrespondenceCandidates( 
				listA, 
				listB, 
				gp.getDifferenceThreshold(), 
				gp.getRatioOfDistance(), 
				gp.getUseAssociatedBeads() );
    	
		pair.setCandidates( candidates );
		
    	// compute ransac and remove inconsistent candidates
    	final ArrayList< PointMatchGeneric< Detection > > inliers = new ArrayList< PointMatchGeneric< Detection > >();

		final Model<?> m;
		
		if ( model == 0 )
			m = new TranslationModel3D();
		else if ( model == 1 )
			m = new RigidModel3D();
		else
			m = new AffineModel3D();
		
		String result = RANSAC.computeRANSAC( candidates, inliers, m, rp.getMaxEpsilon(), rp.getMinInlierRatio(), rp.getMinInlierFactor(), rp.getNumIterations() );

		pair.setInliers( inliers );

    	IOFunctions.println( "(" + new Date( System.currentTimeMillis() ) + "): " + comparison + ": " + result );
		
		return pair;
	}
}