package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import com.razorpay.*;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private MyOrderRepository myOrderRepository;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {

		String userName = principal.getName();
		System.out.println("USERNAME " + userName);

		// get the user using username(email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER " + user);

		model.addAttribute("user", user);

	}

	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");

		return "normal/user_dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {
		try {
			String name = principal.getName();

			User user = this.userRepository.getUserByUserName(name);

			// processing and uploading file..
			if (file.isEmpty()) {
				// if the file is empty then try our message
				System.out.println("File is Empty");
				contact.setImage("contact.png");
			} else {
				// file the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());

				File savefile = new ClassPathResource("static/image").getFile();

				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Image is uploaded");

			}

			contact.setUser(user);

			user.getContacts().add(contact);

			// data save
			this.userRepository.save(user);

			System.out.println("DATA " + contact);

			System.out.println("Added to data base");

			// message success....
			session.setAttribute("message", new com.smart.helper.Message("Your contact is Added !!", "success"));

		} catch (Exception e) {
			// TODO: handle
			System.out.println("Error " + e.getMessage());
			e.printStackTrace();

			// message error
			session.setAttribute("message",
					new com.smart.helper.Message("Something went wrong Try again !!", "danger"));
		}

		return "normal/add_contact_form";
	}

	// show contacts handler
	// per page=5[n]
	// current page=0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {

		m.addAttribute("title", "Show User Contacts");

		// contact ki list ko bhejna hai

		String userName = principal.getName();

		User user = this.userRepository.getUserByUserName(userName);

		// current page
		// Contact per page -5
		PageRequest pageable = PageRequest.of(page, 3);

		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);

		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	// showing particular contact details
	@GetMapping("/{cid}/contact")
	public String showContactDetail(@PathVariable("cid") Integer cid, Model model, Principal principal) {
		System.out.println("CID" + cid);

		Optional<Contact> contactOptional = this.contactRepository.findById(cid);
		Contact contact = contactOptional.get();

		//
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// delete contact handler
	@GetMapping("/delete/{cid}")
	@Transactional
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session,
			Principal principal) {
		System.out.println("CID" + cId);

		Contact contact = this.contactRepository.findById(cId).get();

		// check...

		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);

		this.userRepository.save(user);

		// remove
		// image
		// contact.getImage()

		System.out.println("Deleted");

		session.setAttribute("message", new com.smart.helper.Message("Contact deleted successfully...", "success"));

		return "redirect:/user/show-contacts/0";
	}

	// open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateContact(@PathVariable("cid") Integer cid, Model model) {
		model.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cid).get();

		model.addAttribute("contact", contact);

		return "/normal/update_form";
	}

	// update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {
		try {
			// old contact details
			Contact oldcontactDetail = this.contactRepository.findById(contact.getCid()).get();

			// image..
			if (!file.isEmpty()) {
				// file work
				// rewrite
				// delete old photo

				File deletefile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deletefile, oldcontactDetail.getImage());
				file1.delete();

				// update new photo

				File savefile = new ClassPathResource("static/image").getFile();

				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());

			} else {

				contact.setImage(oldcontactDetail.getImage());
			}

			User user = this.userRepository.getUserByUserName(principal.getName());

			contact.setUser(user);

			this.contactRepository.save(contact);

			session.setAttribute("message", new com.smart.helper.Message("Your Contact is updates", "success"));

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		System.out.println("CONTACT NAME " + contact.getName());
		System.out.println("CONTACT ID" + contact.getCid());

		return "redirect:/user/" + contact.getCid() + "/contact";
	}

	// your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}

	// open settings handler
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}

	// change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		System.out.println("Old Password" + oldPassword);
		System.out.println("New Password" + newPassword);

		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);

		if (this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			// change the password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message",
					new com.smart.helper.Message("Your password is successfully !!", "success"));
		} else {
			// error..
			session.setAttribute("message",
					new com.smart.helper.Message("Please Enter  correct old Password !!", "danger"));
			return "redirect:/user/settings";
		}

		return "redirect:/user/index";
	}

	// creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data,Principal principal) throws RazorpayException {
		// System.out.println("Hey order function executed");
		System.out.println(data);
		int amt = Integer.parseInt(data.get("amount").toString());

		RazorpayClient client = new RazorpayClient("rzp_test_smIFCaaKtmbYmw", "ro0zu3RsomFSro5GB9x5tOZH");

		JSONObject options = new JSONObject();
		options.put("amount", amt * 100);
		options.put("currency", "INR");
		options.put("receipt", "txn_123456");

		// creating new order
		Order order = client.Orders.create(options);
		System.out.println(order);
		
		//save the order in database
		MyOrder myOrder = new MyOrder();
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
		myOrder.setReceipt(order.get("receipt"));
		
		this.myOrderRepository.save(myOrder);
		

		// if you want to can save this to your data...

		return order.toString();
	}
	
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data)
	{
		
		MyOrder myOrder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(myOrder);
		
		System.out.println(data);
		return ResponseEntity.ok(Map.of("msg","updated"));
	}
}
