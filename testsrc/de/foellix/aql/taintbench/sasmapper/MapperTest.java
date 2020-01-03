package de.foellix.aql.taintbench.sasmapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.foellix.aql.helper.FileHelper;

public class MapperTest {
	private static final File ANSWER_DIR = new File("data/answer");

	@BeforeAll
	public static void beforeAll() {
		cleanUp();
	}

	@AfterEach
	public void after() {
		System.out.println("\n\n");
	}

	@AfterAll
	public static void afterAll() {
		cleanUp();
	}

	private void test(String args) {
		boolean noException = true;
		try {
			Mapper.main(args.split(" "));
		} catch (final Exception e) {
			noException = false;
			e.printStackTrace();
		}

		assertTrue(noException);
		assertTrue(Mapper.success);
	}

	private static void cleanUp() {
		FileHelper.deleteDir(ANSWER_DIR);
		ANSWER_DIR.mkdir();
	}

	// Tests
	@Test
	public void test01() {
		test("data/apk/cajino_baidu.apk");
		cleanUp();
	}

	@Test
	public void test02() {
		test("-o data/answer/aql data/apk/cajino_baidu.apk");
	}

	@Test
	public void test03() {
		test("-c fd data/answer/aql/cajino_baidu.xml");
	}

	@Test
	public void test04() {
		test("-c ad data/answer/aql/cajino_baidu.xml");
	}

	@Test
	public void test05() {
		test("-o data/answer/aql -c fd data/answer/aql/cajino_baidu.xml");
	}

	@Test
	public void test06() {
		test("-tb data/json -o data/answer/aql -c fd data/answer/aql/cajino_baidu.xml");
	}

	@Test
	public void test07() {
		test("-o data/answer/aql data/apk/fakebank_android_samp.apk");
	}

	@Test
	public void test08() {
		test("-tb data/json -o data/answer/aql -c fd data/answer/aql/fakebank_android_samp.xml");
	}
}
