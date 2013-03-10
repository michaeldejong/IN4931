package nl.tudelft.in4931.dvgs;

import nl.tudelft.in4931.dvgs.network.JobState;

public interface JobListener {

	void onJobState(JobState job);
	
}
