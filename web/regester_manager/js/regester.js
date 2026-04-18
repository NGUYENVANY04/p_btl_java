async function handleRegister(event) {
    event.preventDefault();

    const registrationData = {
        username: document.getElementById('reg-username').value,
        email: document.getElementById('reg-email').value,
        password: document.getElementById('reg-password').value
    };

    console.log("--- Bắt đầu luồng đăng ký ---");
    console.log("Dữ liệu:", registrationData);

    try {
        const response = await fetch("http://localhost:8080/api/users", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(registrationData)
        });


        console.log("Status:", response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error("API lỗi:", errorText);

            throw new Error(errorText);
        }

        const result = await response.json();
        console.log("=> Đăng ký thành công:", result);

        alert("Đăng ký thành công!");
        showLogin();

    } catch (error) {
        console.error("=> Lỗi:", error);
        alert(error.message); 
    }
}