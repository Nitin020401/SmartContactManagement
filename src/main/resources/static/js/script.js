

console.log("this is script file")

//sidebar toggle jquery code
const toggleSidebar = () => {

	if ($(".sidebar").is(":visible")) {
		//true
		//sidebar band krna hai
		$(".sidebar").css("display", "none");
		$(".content").css("margin-left", "0%");
	}
	else {
		//false
		//sidebar show krna hai
		$(".sidebar").css("display", "block");
		$(".content").css("margin-left", "20%");

	}
};



// search bar jquery code
const search = () => {

	console.log("searching");

	let query = $("#search-input").val();

	//	let query=document.getElementById('search-input');

	if (query == "") {
		$(".search-result").hide();
	}
	else {
		console.log(query);

		//sending request to server

		let url = `http://localhost:8080/search/${query}`;

		fetch(url)
			.then((response) => {
				return response.json();
			})
			.then((data) => {
				//data
				//console.log(data);


				let text = `<div class='list-group'>`;



				data.forEach((contact) => {
					text += `<a href='/user/${contact.cid}/contact' class='list-group-item list-group-item-action'>${contact.name}</a>`
				});


				text += `</div>`;

				$(".search-result").html(text);
				$(".search-result").show();



			});

		$(".search-result").show();
	}

};



//first request - to server to create order

const paymentStart = () => {
	console.log("payment started..")
	let amount = $("#payment_field").val();
	console.log(amount);
	if (amount == "" || amount == null) {
		//alert("amount is required !!");
		swal("Failed !!", "amount is required !!", "error");
		return;
	}

	//code...
	//We will use ajax to send request to server to create order - jquery

	$.ajax(
		{
			url: "/user/create_order",
			data: JSON.stringify({ amount: amount, info: 'order_request' }),
			contentType: "application/json",
			type: "POST",
			dataType: "json",
			success: function (response) {
				//invoked where success
				console.log(response);
				if (response.status == "created") {
					//open payment form
					let options = {
						key: "rzp_test_smIFCaaKtmbYmw",
						amount: response.amount,
						currency: "INR",
						name: "Smart Contact Manager",
						description: "Donation",
						image: "https://cdn.pixabay.com/photo/2020/07/01/12/58/icon-5359553_1280.png",
						order_id: response.id,
						handler: function (response) {
							console.log(response.razorpay_payment_id);
							console.log(response.razorpay_order_id);
							console.log(response.razorpay_signature);
							console.log("payment successfull !!");
							//alert("congrates !! Payment successfull !!");

							updatePaymentServer(response.razorpay_payment_id, response.razorpay_order_id, "paid");


						},
						prefill: {
							name: "",
							email: "",
							contact: "",
						},
						notes: {
							address: "LearnCodeWith Nitin",
						},
						theme: {
							color: "#3399cc",
						},
					};

					const rzp = new Razorpay(options);
					rzp.on("payment.failed", function (response) {
						console.log(response.error.code);
						console.log(response.error.description);
						console.log(response.error.source);
						console.log(response.error.step);
						console.log(response.error.reason);
						console.log(response.error.metadata.order_id);
						console.log(response.error.metadata.payment_id);
						//alert("Oops payment failed !!");
						swal("Failed !!", "Oops payment failed !!", "error");

					});

					rzp.open();


				}
			},
			error: function (error) {
				//invoked when error
				console.log(error);
				alert("something went wrong !!");
			}
		}
	)
};

//update payment function call

 function updatePaymentServer(payment_id, order_id, status)
{
	$.ajax({
		url: "/user/update_order",
		data: JSON.stringify({ payment_id: payment_id, order_id: order_id, status: status }),
		contentType: "application/json",
		type: "POST",
		dataType: "json",
		success: function (response) {
			swal("Good job!", "congrates !! Payment successfull !!", "success");
		},
		error: function (error) {
			swal("Failed !!", "Your payment is successfull, but we did not get on server , we will contact you as soon as possible", "error");
		 },
	})
}



