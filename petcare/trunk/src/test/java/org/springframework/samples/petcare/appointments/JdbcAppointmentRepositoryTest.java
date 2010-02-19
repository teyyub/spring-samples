package org.springframework.samples.petcare.appointments;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class JdbcAppointmentRepositoryTest {

	private EmbeddedDatabase database;

	private JdbcAppointmentRepository appointmentRepository;

	private PlatformTransactionManager transactionManager;
	
	private JdbcTemplate template;
	
	@Before
	public void setUp() {
		database = new EmbeddedDatabaseBuilder().
			setType(EmbeddedDatabaseType.H2).
			addScript("schema.sql").
			addScript("data.sql").build();
		appointmentRepository = new JdbcAppointmentRepository(database);
		transactionManager = new DataSourceTransactionManager(database);
		template = new JdbcTemplate(database);
	}

	@After
	public void tearDown() {
		database.shutdown();
	}

	@Test
	public void testGetAppointmentsForDay() {
		AppointmentCalendar calendar = appointmentRepository.getAppointmentsForDay(new LocalDate(2010, 02, 16));
		assertEquals(new LocalDate(2010, 02, 16), calendar.getDay());
		assertEquals("Macy", calendar.getAppointments().get(0).get(0).getPatient());
		assertEquals("Lil Jerry", calendar.getAppointments().get(8).get(1).getPatient());
	}
	
	@Test
	public void testCreateAppointment() {
		TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
		NewAppointment appointment = new NewAppointment();
		appointment.setDay(new LocalDate());
		appointment.setTime(new LocalTime(8, 0));
		appointment.setDoctorId(1L);
		appointment.setPatientId(1L);
		appointment.setReason("Checkup");
		long id = appointmentRepository.createAppointment(appointment);
		assertEquals(3L, id);
		Map<String, Object> row = template.queryForMap("select * from Appointment where id = ?", id);
		assertEquals(new LocalDate().toDateTime(new LocalTime(8, 0)).toDate(), row.get("STARTTIME"));
		assertEquals(new LocalDate().toDateTime(new LocalTime(9, 0)).toDate(), row.get("ENDTIME"));
		assertEquals(1L, row.get("DOCTORID"));
		assertEquals(1L, row.get("PATIENTID"));
		assertEquals("Checkup", row.get("REASON"));		
		transactionManager.commit(status);
	}

}
