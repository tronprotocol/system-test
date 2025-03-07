package tron.trident.proposal;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.tron.trident.proto.Response;
import tron.trident.utils.TestBase;
import java.util.Random;

@Slf4j
public class proposalBaseInfo extends TestBase {

    @Test(enabled = true)
    public void test01getPaginatedProposalList() throws Exception {

        Random random = new Random();
        int[] limits = {5, 10, 20, 50, 100, 500, 1000};
        for (int i = 0; i < 10; i++) {
            int offset = (random.nextInt(196) + 1) * 100;
            int limit = limits[random.nextInt(limits.length)];
            Response.ProposalList proposalList = wrapper.getPaginatedProposalList(offset, limit);
            logger.info(String.valueOf(offset),limit);
            logger.info(String.valueOf(proposalList.getProposalsCount()));
            Assert.assertTrue(proposalList.getProposalsCount() <= limit);
            Assert.assertEquals(proposalList.getProposals(0).getProposalId(), offset + 1);
        }

        //offset >= exchange amount, result is empty
        Response.ProposalList proposalList = wrapper.getPaginatedProposalList(50000, 3);
        Assert.assertEquals(proposalList.getProposalsCount(), 0);
        //limit max = 1000
        Response.ProposalList proposalList1 = wrapper.getPaginatedProposalList(0, 50000);
        Assert.assertEquals(proposalList1.getProposalsCount(), 1000);

    }
}
